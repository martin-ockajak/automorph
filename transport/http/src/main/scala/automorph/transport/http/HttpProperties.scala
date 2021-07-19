package automorph.transport.http

import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.Base64

final case class HttpProperties[Source](
  source: Option[Source] = None,
  method: Option[String] = None,
  headers: Seq[(String, String)] = Seq(),
  webSocket: Boolean = false
) {

  private val charset = StandardCharsets.UTF_8
  private val authorizationBasic = "Basic"
  private val authorizationBearer = "Bearer"
  private val headerAuthorization = "Authorization"
  private val headerContentLength = "Content-Length"
  private val headerContentType = "Content-Type"
  private val headerProxyAuthorization = "Proxy-Authorization"
  private val headerSetCookie = "Set-Cookie"

  def authorization: Option[String] = header(headerAuthorization)

  def authBasic: Option[String] = auth(authorizationBasic)

  def authBearer: Option[String] = auth(authorizationBearer)

  def contentType: Option[String] = header(headerContentType)

  def contentLength: Option[String] = header(headerContentLength)

  def cookies: Map[String, Option[String]] = headers(headerSetCookie).map { header =>
    header.split("=", 2).map(_.trim) match {
      case Array(name) => name -> None
      case Array(name, value) => name -> Some(value)
    }
  }.toMap

  def cookie(name: String): Option[String] = cookies.get(name).flatten

  def header(name: String): Option[String] = headers.find(_._1 == name).map(_._2)

  def headers(name: String): Seq[String] = headers.filter(_._1 == name).map(_._2)

  def proxyAuthorization: Option[String] = header(headerProxyAuthorization)

  def header(name: String, value: String): HttpProperties[Source] = header(name, value, false)

  def header(name: String, value: String, replace: Boolean = false): HttpProperties[Source] = {
    val originalHeaders = if (replace) headers.filter(_._1 != name) else headers
    copy(headers = originalHeaders :+ (name -> value))
  }

  def authBasic(user: String, password: String): HttpProperties[Source] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerAuthorization, s"$authorizationBasic $value")
  }

  def authBasic(token: String): HttpProperties[Source] =
    header(headerAuthorization, s"$authorizationBasic $token")

  def authBearer(token: String): HttpProperties[Source] =
    header(headerAuthorization, s"$authorizationBearer $token")

  def proxyAuthBasic(user: String, password: String): HttpProperties[Source] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerProxyAuthorization, s"$authorizationBasic $value")
  }

  def proxyAuthBasic(token: String): HttpProperties[Source] =
    header(headerProxyAuthorization, s"$authorizationBasic $token")

  def proxyAuthBearer(token: String): HttpProperties[Source] =
    header(headerProxyAuthorization, s"$authorizationBearer $token")

  def auth(method: String): Option[String] =
    headers(headerAuthorization).find(_.trim.startsWith(method)).flatMap(_.split(" ") match {
      case Array(_) => None
      case Array(_, value) => Some(value)
    })

}
