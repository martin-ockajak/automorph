package automorph.transport.http

import automorph.protocol.jsonrpc.ErrorType
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
final case class Http[Source](
  source: Option[Source] = None,
  method: Option[String] = None,
  scheme: Option[String] = None,
  authority: Option[String] = None,
  path: Option[String] = None,
  query: Option[String] = None,
  fragment: Option[String] = None,
  headers: Seq[(String, String)] = Seq(),
  followRedirects: Boolean = true,
  readTimeout: Duration = FiniteDuration(30, TimeUnit.SECONDS)
) {

  private val charset = StandardCharsets.UTF_8
  private val headerAuthorizationBasic = "Basic"
  private val headerAuthorizationBearer = "Bearer"
  private val headerAuthorization = "Authorization"
  private val headerContentLength = "Content-Length"
  private val headerContentType = "Content-Type"
  private val headerCookie = "Cookie"
  private val headerProxyAuthorization = "Proxy-Authorization"
  private val headerSetCookie = "Set-Cookie"

  /** `Authorization` header value. */
  def authorization: Option[String] = header(headerAuthorization)

  /** `Authorization: Basic` header value. */
  def authorizationBasic: Option[String] = authorization(headerAuthorization, headerAuthorizationBasic)

  /** `Authorization: Bearer` header value. */
  def authorizationBearer: Option[String] = authorization(headerAuthorization, headerAuthorizationBearer)

  /** `Content-Type` header value. */
  def contentType: Option[String] = header(headerContentType)

  /** `Content-Length` header value. */
  def contentLength: Option[String] = header(headerContentLength)

  /** Cookie names and values. */
  def cookies: Map[String, Option[String]] = cookies(headerCookie)

  /** Set-Cookie names and values. */
  def setCookies: Map[String, Option[String]] = cookies(headerSetCookie)

  /**
   * Cookie value.
   *
   * @param name cookie name
   * @return cookie value
   */
  def cookie(name: String): Option[String] = cookies.get(name).flatten

  /**
   * First header value.
   *
   * @param name header name
   * @return first header value
   */
  def header(name: String): Option[String] = headers.find(_._1 == name).map(_._2)

  /**
   * Header values.
   *
   * @param name header name
   * @return header values
   */
  def headers(name: String): Seq[String] = headers.filter(_._1 == name).map(_._2)

  /** `Proxy-Authorization` header value. */
  def proxyAuthorization: Option[String] = header(headerProxyAuthorization)

  /** `Proxy-Authorization: Basic` header value. */
  def proxyAuthorizationBasic: Option[String] = authorization(headerProxyAuthorization, headerAuthorizationBasic)

  /** `Proxy-Authorization: Bearer` header value. */
  def proxyAuthorizationBearer: Option[String] = authorization(headerProxyAuthorization, headerAuthorizationBearer)

  /** Request URL. */
  def url: Option[URI] = (scheme, authority, path, query, fragment) match {
    case (Some(scheme), Some(authority), Some(path), query, fragment) =>
      Some(new URI(scheme, authority, path, query.orNull, fragment.orNull))
    case _ => None
  }

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP properties
   */
  def authorizationBasic(user: String, password: String): Http[Source] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def authorizationBasic(token: String): Http[Source] =
    header(headerAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def authorizationBearer(token: String): Http[Source] =
    header(headerAuthorization, s"$headerAuthorizationBearer $token")

  /**
   * Set request cookies.
   *
   * @param entries cookie names and values
   * @return HTTP properties
   */
  def cookies(entries: (String, String)*): Http[Source] =
    cookies(entries, headerCookie)

  /**
   * Add message header.
   *
   * @param name header name
   * @param value header value
   * @return HTTP properties
   */
  def header(name: String, value: String): Http[Source] = header(name, value, false)

  /**
   * Add or replace message header.
   *
   * @param name header name
   * @param value header value
   * @param replace replace all existing headers with the specied name
   * @return HTTP properties
   */
  def header(name: String, value: String, replace: Boolean): Http[Source] = {
    val originalHeaders = if (replace) headers.filter(_._1 != name) else headers
    copy(headers = originalHeaders :+ (name -> value))
  }

  /**
   * Set message headers.
   *
   * @param entries header names and values
   * @return HTTP properties
   */
  def headers(entries: (String, String)*): Http[Source] =
    headers(entries, false)

  /**
   * Set message headers.
   *
   * @param entries header names and values
   * @param replace replace all existing headers with specified names
   * @return HTTP properties
   */
  def headers(entries: Iterable[(String, String)], replace: Boolean): Http[Source] = {
    val entryNames = entries.map { case (name, _) => name }.toSet
    val originalHeaders = if (replace) headers.filter { case (name, _) => !entryNames.contains(name) } else headers
    copy(headers = originalHeaders ++ entries)
  }

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP properties
   */
  def proxyAuthBasic(user: String, password: String): Http[Source] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def proxyAuthBasic(token: String): Http[Source] =
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Proxy-Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def proxyAuthBearer(token: String): Http[Source] =
    header(headerProxyAuthorization, s"$headerAuthorizationBearer $token")

  /**
   * Set request URL query string.
   *
   * @param value URL query string
   * @return HTTP properties
   */
  def query(value: String): Http[Source] = this.copy(query = Some(value))

  /**
   * Set request URL query parameters.
   *
   * @param entries query parameter names and values
   * @return HTTP properties
   */
  def parameters(entries: (String, String)*): Http[Source] = {
    val components = entries.map { case (name, value) => s"$name=$value" }
    query(s"?${components.mkString("&")}")
  }

  /**
   * Set response cookies.
   *
   * @param entries cookie names and values
   * @return HTTP properties
   */
  def setCookies(values: (String, String)*): Http[Source] =
    cookies(values, headerSetCookie)

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP properties
   */
  def url(url: String): Http[Source] = this.url(new URI(url))

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP properties
   */
  def url(url: URI): Http[Source] =
    copy(
      scheme = Some(url.getScheme),
      authority = Some(url.getAuthority),
      path = Some(url.getPath),
      query = Some(url.getQuery),
      fragment = Some(url.getFragment)
    )

  private def authorization(header: String, method: String): Option[String] =
    headers(header).find(_.trim.startsWith(method)).flatMap(_.split(" ") match {
      case Array(_, value) => Some(value)
      case _ => None
    })

  private def cookies(values: Iterable[(String, String)], headerName: String): Http[Source] = {
    val headerValue = (headers(headerName) ++ values.map { case (name, value) => s"$name=$value" }).mkString("; ")
    header(headerName, headerValue, true)
  }

  private def cookies(headerName: String): Map[String, Option[String]] = {
    headers(headerName).flatMap { header =>
      header.split("=", 2).map(_.trim) match {
        case Array(name, value) => Some(name -> Some(value))
        case Array(name) => Some(name -> None)
        case _ => None
      }
    }.toMap
  }
}

case object Http {

  /** Default JSON-RPC error to HTTP status code mapping. */
  val defaultErrorStatusCode: Int => Int = Map(
    ErrorType.ParseError -> 400,
    ErrorType.InvalidRequest -> 400,
    ErrorType.MethodNotFound -> 501,
    ErrorType.InvalidParams -> 400,
    ErrorType.InternalError -> 500,
    ErrorType.IOError -> 500,
    ErrorType.ApplicationError -> 500
  ).withDefaultValue(500).map { case (errorType, statusCode) =>
    errorType.code -> statusCode
  }
}
