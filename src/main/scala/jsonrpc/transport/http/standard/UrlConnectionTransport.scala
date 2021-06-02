package jsonrpc.transport.http.standard

import java.net.{HttpURLConnection, URL, URLConnection}
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.effect.standard.NoEffect.Identity
import jsonrpc.spi.Transport
import jsonrpc.transport.http.standard.UrlConnectionTransport.HttpProperties
import scala.collection.immutable.ArraySeq

/**
 * URL connection HTTP transport.
 *
 * @param url HTTP endpoint URL
 * @param bufferSize input stream reading buffer size
 */
case class UrlConnectionTransport(
  url: URL,
  bufferSize: Int = 4096
) extends Transport[Identity, HttpProperties]:

  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val contentType = "application/json"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
  private val connection = connect()

  override def call(request: ArraySeq.ofByte, context: HttpProperties): Identity[ArraySeq.ofByte] =
    send(request, context)
    val inputStream = connection.getInputStream
    try
      inputStream.toArraySeq(bufferSize)
    finally
      inputStream.close()

  override def notify(request: ArraySeq.ofByte, context: HttpProperties): Identity[Unit] =
    send(request, context)
    ()

  private def send(request: ArraySeq.ofByte, context: HttpProperties): Unit =
    val outputStream = connection.getOutputStream
    try
      setProperties(request, context)
      outputStream.write(request.unsafeArray)
    finally
      clearProperties(context)
      outputStream.close()

  private def setProperties(request: ArraySeq.ofByte, context: HttpProperties): Unit =
    // Validate HTTP request properties
    require(httpMethods.contains(context.method), s"Invalid HTTP method: ${context.method}")

    // Set HTTP request context
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, contentType)
    connection.setRequestProperty(acceptHeader, contentType)
    connection.setRequestMethod(context.method)
    connection.setConnectTimeout(context.connectTimeout)
    connection.setReadTimeout(context.readTimeout)
    context.headers.foreach((key, value) => connection.setRequestProperty(key, value))

  private def clearProperties(context: HttpProperties): Unit =
    context.headers.foreach((key, _) => connection.setRequestProperty(key, null))

  private def connect(): HttpURLConnection =
    // Open new HTTP connection
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection

object UrlConnectionTransport:

  /**
   * HTTP properties.
   *
   * @see [[https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html Documentation]]
   * @param method HTTP method (GET, POST, PUT, DELETE, HEAD, OPTIONS)
   * @param headers HTTP headers
   * @param followRedirects automatically follow HTTP redirects
   * @param connectTimeout connection timeout (milliseconds)
   * @param readTimeout read timeout (milliseconds)
   */
  case class HttpProperties(
    method: String = "POST",
    headers: Map[String, String] = Map.empty,
    followRedirects: Boolean = true,
    connectTimeout: Int = 30000,
    readTimeout: Int = 30000
  )
