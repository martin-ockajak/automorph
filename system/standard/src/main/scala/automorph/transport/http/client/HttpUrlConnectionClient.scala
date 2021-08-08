package automorph.transport.http.client

import automorph.log.Logging
import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.system.IdentitySystem
import automorph.transport.http.Http
import automorph.transport.http.client.HttpUrlConnectionClient.{Context, EffectValue}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import java.net.{HttpURLConnection, URI}
import java.security.Identity
import scala.collection.immutable.ArraySeq
import scala.concurrent.duration.Duration
import scala.util.{Try, Using}

/**
 * URL connection client transport plugin using HTTP as message transport protocol.
 *
 * The client uses the supplied RPC request as HTTP request body and returns HTTP response body as a result.
 *
 * This transport uses blocking I/O operations so it is strongly recommended to supply
 * a suitable blocking effect creation function when using an asynchronous effect system.
 *
 * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html API]]
 * @constructor Creates an HTTP URL connection client transport plugin.
 * @param url HTTP server endpoint URL
 * @param method HTTP method
 * @param system effect system plugin
 * @param blockingEffect creates an effect from specified blocking function
 * @tparam Effect effect type
 */
final case class HttpUrlConnectionClient[Effect[_]](
  url: URI,
  method: String,
  system: EffectSystem[Effect] = IdentitySystem(),
  blockingEffect: Option[(() => EffectValue) => Effect[EffectValue]] = None
) extends ClientMessageTransport[Effect, Context] with Logging {

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  require(httpMethods.contains(method), s"Invalid HTTP method: $method")

  override def call(
    request: ArraySeq.ofByte,
    mediaType: String,
    context: Option[Context]
  ): Effect[ArraySeq.ofByte] =
    system.map(
      system.flatMap(
        send(request, mediaType, context),
        { case (connection, _) =>
          blocking {
            logger.trace("Receiving HTTP response", Map("URL" -> url))
            Try(Using.resource(connection.getInputStream)(Bytes.inputStream.from)).mapFailure { error =>
              logger.error("Failed to receive HTTP response", error, Map("URL" -> url))
              error
            }.map { response =>
              logger.debug(
                "Received HTTP response",
                Map("URL" -> url, "Status" -> connection.getResponseCode, "Size" -> response.length)
              )
              connection -> response
            }.get
          }
        }
      ),
      { case (_, response) =>
        response
      }
    )

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] =
    system.map(send(request, mediaType, context), _ => ())

  override def defaultContext: Context = HttpUrlConnectionClient.defaultContext.copy(method = Some(method))

  override def close(): Unit = ()

  private def send(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[EffectValue] =
    blocking {
      logger.trace("Sending HTTP request", Map("URL" -> url, "Size" -> request.length))
      val properties = context.getOrElse(defaultContext)
      val connection = connect(properties)
      val httpMethod = setProperties(connection, request, mediaType, properties)
      val outputStream = connection.getOutputStream
      val write = Using(outputStream)(_.write(request.unsafeArray))
      clearProperties(connection, properties)
      write.mapFailure { error =>
        logger.error(
          "Failed to send HTTP request",
          error,
          Map("URL" -> url, "Method" -> httpMethod, "Size" -> request.length)
        )
        error
      }.map { _ =>
        logger.debug("Sent HTTP request", Map("URL" -> url, "Method" -> httpMethod, "Size" -> request.length))
        connection -> ArraySeq.ofByte(Array.empty)
      }.get
    }

  private def setProperties(
    connection: HttpURLConnection,
    request: ArraySeq.ofByte,
    mediaType: String,
    properties: Context
  ): String = {
    val httpMethod = properties.method.getOrElse(method)
    require(httpMethods.contains(httpMethod), s"Invalid HTTP method: $httpMethod")
    connection.setRequestMethod(httpMethod)
    connection.setConnectTimeout(properties.readTimeout.toMillis.toInt)
    connection.setReadTimeout(properties.readTimeout match {
      case Duration.Inf => 0
      case duration => duration.toMillis.toInt
    })
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, mediaType)
    connection.setRequestProperty(acceptHeader, mediaType)
    properties.path.getOrElse(url.getPath)
    properties.headers.foreach { case (key, value) =>
      connection.setRequestProperty(key, value)
    }
    httpMethod
  }

  private def clearProperties(connection: HttpURLConnection, properties: Context): Unit =
    properties.headers.foreach { case (key, _) =>
      connection.setRequestProperty(key, null)
    }

  private def connect(properties: Context): HttpURLConnection = {
    val connectionUrl = properties.url.getOrElse(url)
    val connection = url.toURL.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection
  }

  private def defaultBlockingEffect(
    blocking: (() => EffectValue)
  ): Effect[EffectValue] = Try(blocking()).pureFold(
    error => system.failed(error),
    result => system.pure(result)
  )

  private def blocking(value: => EffectValue): Effect[EffectValue] =
    blockingEffect.getOrElse(defaultBlockingEffect)(() => value)
}

case object HttpUrlConnectionClient {

  /** Request context type. */
  type Context = Http[HttpURLConnection]

  /** Effect value type. */
  type EffectValue = (HttpURLConnection, ArraySeq.ofByte)

  implicit val defaultContext: Context = Http()
}
