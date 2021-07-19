package automorph.transport.http

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * HTTP message properties.
 *
 * @see [[https://datatracker.ietf.org/doc/html/rfc7232 HTTP specification]]
 * @param source source properties provided by the specific message transport plugin
 * @param method request method
 * @param scheme request URL scheme
 * @param authority request URL authority
 * @param path request URL path
 * @param query request URL query
 * @param fragment request URL fragment
 * @param headers message headers
 * @param followRedirects automatically follow redirects if true
 * @param readTimeout response read timeout
 * @tparam Source specific message transport plugin source properties type
 */
final case class HttpProperties[Source](
  source: Option[Source] = None,
  method: Option[String] = None,
  scheme: Option[String] = None,
  authority: Option[String] = None,
  path: Option[String] =None,
  query: Option[String] = None,
  fragment: Option[String] = None,
  headers: Seq[(String, String)] = Seq(),
  followRedirects: Boolean = true,
  readTimeout: Duration = FiniteDuration(30, TimeUnit.SECONDS)
) {

  private val charset = StandardCharsets.UTF_8
  private val authorizationBasic = "Basic"
  private val authorizationBearer = "Bearer"
  private val headerAuthorization = "Authorization"
  private val headerContentLength = "Content-Length"
  private val headerContentType = "Content-Type"
  private val headerCookie = "Cookie"
  private val headerProxyAuthorization = "Proxy-Authorization"
  private val headerSetCookie = "Set-Cookie"

  def authorization: Option[String] = header(headerAuthorization)

  def authBasic: Option[String] = auth(authorizationBasic)

  def authBearer: Option[String] = auth(authorizationBearer)

  def contentType: Option[String] = header(headerContentType)

  def contentLength: Option[String] = header(headerContentLength)

  def cookies: Map[String, Option[String]] = (headers(headerCookie) ++ headers(headerSetCookie)).flatMap { header =>
    header.split("=", 2).map(_.trim) match {
      case Array(name, value) => Some(name -> Some(value))
      case Array(name) => Some(name -> None)
      case _ => None
    }
  }.toMap

  def cookie(name: String): Option[String] = cookies.get(name).flatten

  def header(name: String): Option[String] = headers.find(_._1 == name).map(_._2)

  def headers(name: String): Seq[String] = headers.filter(_._1 == name).map(_._2)

  def proxyAuthorization: Option[String] = header(headerProxyAuthorization)

  def url: Option[URI] = (scheme, authority, path, query, fragment) match {
    case (Some(scheme), Some(authority), Some(path), query, fragment) =>
      Some(new URI(scheme, authority, path, query.orNull, fragment.orNull))
    case _ => None
  }

  def authBasic(user: String, password: String): HttpProperties[Source] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerAuthorization, s"$authorizationBasic $value")
  }

  def authBasic(token: String): HttpProperties[Source] =
    header(headerAuthorization, s"$authorizationBasic $token")

  def authBearer(token: String): HttpProperties[Source] =
    header(headerAuthorization, s"$authorizationBearer $token")

  def cookies(values: (String, String)*): HttpProperties[Source] =
    cookies(values, false)

  def header(name: String, value: String): HttpProperties[Source] = header(name, value, false)

  def header(name: String, value: String, replace: Boolean): HttpProperties[Source] = {
    val originalHeaders = if (replace) headers.filter(_._1 != name) else headers
    copy(headers = originalHeaders :+ (name -> value))
  }

  def headers(values: (String, String)*): HttpProperties[Source] =
    headers(values, false)

  def headers(values: Seq[(String, String)], replace: Boolean): HttpProperties[Source] = {
    val originalHeaders = if (replace) headers.filter { case (name, _) => !values.contains(name) } else headers
    copy(headers = originalHeaders ++ values)
  }

  def proxyAuthBasic(user: String, password: String): HttpProperties[Source] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerProxyAuthorization, s"$authorizationBasic $value")
  }

  def proxyAuthBasic(token: String): HttpProperties[Source] =
    header(headerProxyAuthorization, s"$authorizationBasic $token")

  def proxyAuthBearer(token: String): HttpProperties[Source] =
    header(headerProxyAuthorization, s"$authorizationBearer $token")

  def setCookies(values: (String, String)*): HttpProperties[Source] =
    cookies(values, true)

  def url(url: String): HttpProperties[Source] = this.url(new URI(url))

  def url(url: URI): HttpProperties[Source] =
    copy(
      scheme = Some(url.getScheme),
      authority = Some(url.getAuthority),
      path = Some(url.getPath),
      query = Some(url.getQuery),
      fragment = Some(url.getFragment)
    )

  private def auth(method: String): Option[String] =
    headers(headerAuthorization).find(_.trim.startsWith(method)).flatMap(_.split(" ") match {
      case Array(_, value) => Some(value)
      case _ => None
    })

  private def cookies(values: Seq[(String, String)], set: Boolean): HttpProperties[Source] = {
    val headerName = if (set) headerSetCookie else headerCookie
    val headerValue = (headers(headerName) ++ values.map { case (name, value) => s"$name=$value" }).mkString("; ")
    header(headerName, headerValue, true)
  }
}
