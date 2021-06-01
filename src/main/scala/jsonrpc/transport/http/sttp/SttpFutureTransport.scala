package jsonrpc.transport.http.sttp

import java.net.{HttpURLConnection, URL, URLConnection}
import java.nio.charset.StandardCharsets
import jsonrpc.core.EncodingOps.asArraySeq
import jsonrpc.spi.{Effect, Transport}
import jsonrpc.transport.http.sttp.SttpFutureTransport.HttpProperties
import org.asynchttpclient.AsyncHttpClientConfig
import scala.collection.immutable.ArraySeq
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.{SttpApi, SttpBackend}
import sttp.model.Uri
import sttp.model.Method
import sttp.client3.Request
import sttp.client3.RequestT
import sttp.client3.Identity
import sttp.client3.Empty

/**
 * STTP HTTP transport using AsyncHttpClientFutureBackend.
 *
 * @param url HTTP endpoint URL
 * @param backend STTP backend
 * @param effect effect system plugin
 */
case class SttpFutureTransport[Outcome[_], Capabilities](
  url: Uri,
  backend: SttpBackend[Outcome, Capabilities],
  effect: Effect[Outcome]
) extends Transport[Outcome, HttpProperties] with SttpApi:

  private val charset = StandardCharsets.UTF_8
  private val contentLengthHeader = "Content-Length"
  private val contentTypeHeader = "Content-Type"
  private val acceptHeader = "Accept"
  private val contentType = "application/json"
  private val httpMethods = Set("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")

  override def call(request: ArraySeq.ofByte, context: HttpProperties): Outcome[ArraySeq.ofByte] =
    val httpRequest = createHttpRequest(request, context)
    effect.flatMap(httpRequest.send(backend), _.body.fold(
      error => effect.failed(IllegalStateException(error)),
      response => effect.pure(response.asArraySeq)
    ))

  override def notify(request: ArraySeq.ofByte, context: HttpProperties): Outcome[Unit] =
    val httpRequest = createHttpRequest(request, context)
    effect.flatMap(httpRequest.send(backend), _.body.fold(
      error => effect.failed(IllegalStateException(error)),
      response => effect.pure(())
    ))

  private def createHttpRequest(request: ArraySeq.ofByte, context: HttpProperties): RequestT[Identity, Either[String, Array[Byte]], Any] =
    basicRequest.copy[Identity, Either[String, String], Any](uri = url, method = context.method).body(request.unsafeArray).response(asByteArray)

//  private def setProperties(request: ArraySeq.ofByte, context: Option[HttpProperties]): Unit =
//    // Validate HTTP request properties
//    val properties = context.getOrElse(HttpProperties())
//    require(httpMethods.contains(properties.method), s"Invalid HTTP method: ${properties.method}")
//
//    // Set HTTP request properties
//    connection.setRequestProperty(contentLengthHeader, request.size.toString)
//    connection.setRequestProperty(contentTypeHeader, contentType)
//    connection.setRequestProperty(acceptHeader, contentType)
//    connection.setRequestMethod(properties.method)
//    connection.setConnectTimeout(properties.connectTimeout)
//    connection.setReadTimeout(properties.readTimeout)
//    properties.headers.foreach((key, value) => connection.setRequestProperty(key, value))
//
//  private def clearProperties(context: Option[HttpProperties]): Unit =
//    val properties = context.getOrElse(HttpProperties())
//    properties.headers.foreach((key, _) => connection.setRequestProperty(key, null))
//
//  private def connect(): HttpURLConnection =
//    // Open new HTTP connection
//    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
//    connection.setDoOutput(true)
//    connection

object SttpFutureTransport:

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
    method: Method = Method.POST,
    headers: Map[String, String] = Map.empty,
    followRedirects: Boolean = true,
    connectTimeout: Int = 30000,
    readTimeout: Int = 30000
  )
