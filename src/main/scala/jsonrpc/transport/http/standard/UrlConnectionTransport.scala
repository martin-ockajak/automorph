package jsonrpc.transport.http.standard

import jsonrpc.JsonRpcHandler
import jsonrpc.spi.{Codec, Effect, Transport}
import scala.collection.immutable.ArraySeq
import java.net.{HttpURLConnection, URL, URLConnection}
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.core.Request
import jsonrpc.transport.http.standard.UrlConnection.HttpProperties
import jsonrpc.effect.standard.NoEffect.Identity

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

  override def call(request: ArraySeq.ofByte, context: Option[HttpProperties]): Identity[ArraySeq.ofByte] =
    send(request, context)
    val inputStream = connection.getInputStream
    try
      inputStream.toArraySeq(bufferSize)
    finally
      inputStream.close()

  override def notify(request: ArraySeq.ofByte, context: Option[HttpProperties]): Identity[Unit] =
    send(request, context)
    ()

  private def send(request: ArraySeq.ofByte, context: Option[HttpProperties]): Unit =
    val outputStream = connection.getOutputStream
    try
      setProperties(request, context)
      outputStream.write(request.unsafeArray)
    finally
      clearProperties(context)
      outputStream.close()

  private def setProperties(request: ArraySeq.ofByte, context: Option[HttpProperties]): Unit =
    // Validate HTTP request properties
    val properties = context.getOrElse(HttpProperties())
    require(httpMethods.contains(properties.method), s"Invalid HTTP method: ${properties.method}")

    // Set HTTP request properties
    connection.setRequestProperty(contentLengthHeader, request.size.toString)
    connection.setRequestProperty(contentTypeHeader, contentType)
    connection.setRequestProperty(acceptHeader, contentType)
    connection.setRequestMethod(properties.method)
    connection.setConnectTimeout(properties.connectTimeout)
    connection.setReadTimeout(properties.readTimeout)
    properties.headers.foreach((key, value) => connection.setRequestProperty(key, value))

  private def clearProperties(context: Option[HttpProperties]): Unit =
    val properties = context.getOrElse(HttpProperties())
    properties.headers.foreach((key, _) => connection.setRequestProperty(key, null))

  private def connect(): HttpURLConnection =
    // Open new HTTP connection
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setDoOutput(true)
    connection

object UrlConnection:

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
