package automorph.transport.http

import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, ParseErrorException, ServerErrorException}
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException}
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.duration.Duration

/**
 * HTTP message properties.
 *
 * Message transport plugins must set message properties based on their origin in the descending order of priority:
 * - This class
 * - Base
 * - Defaults
 *
 * @see [[https://datatracker.ietf.org/doc/html/rfc7232 HTTP specification]]
 * @param method request method
 * @param scheme request URL scheme
 * @param authority request URL authority
 * @param path request URL path
 * @param fragment request URL fragment
 * @param headers message headers
 * @param followRedirects automatically follow redirects if true
 * @param readTimeout response read timeout
 * @param base base properties defined by the specific message transport plugin
 * @tparam Base specific message transport plugin base properties type
 */
final case class Http[Base](
  method: Option[String] = None,
  scheme: Option[String] = None,
  authority: Option[String] = None,
  path: Option[String] = None,
  parameters: Seq[(String, String)] = Seq.empty,
  fragment: Option[String] = None,
  headers: Seq[(String, String)] = Seq.empty,
  followRedirects: Option[Boolean] = None,
  readTimeout: Option[Duration] = None,
  base: Option[Base] = None
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

  /**
   * First URL query parameter value.
   *
   * @param name query parameter name
   * @return first query parameter value
   */
  def parameter(name: String): Option[String] = parameters.find(_._1 == name).map(_._2)

  /**
   * URL query parameter values.
   *
   * @param name query parameter name
   * @return query parameter values
   */
  def parameters(name: String): Seq[String] = parameters.filter(_._1 == name).map(_._2)

  /** `Proxy-Authorization` header value. */
  def proxyAuthorization: Option[String] = header(headerProxyAuthorization)

  /** `Proxy-Authorization: Basic` header value. */
  def proxyAuthorizationBasic: Option[String] = authorization(headerProxyAuthorization, headerAuthorizationBasic)

  /** `Proxy-Authorization: Bearer` header value. */
  def proxyAuthorizationBearer: Option[String] = authorization(headerProxyAuthorization, headerAuthorizationBearer)

  /** Request URL query. */
  def query: Option[String] = parameters match {
    case Seq() => None
    case _ => Some(s"?${parameters.map { case (name, value) => s"$name=$value" }.mkString("&")}")
  }

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
  def authorizationBasic(user: String, password: String): Http[Base] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def authorizationBasic(token: String): Http[Base] =
    header(headerAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def authorizationBearer(token: String): Http[Base] =
    header(headerAuthorization, s"$headerAuthorizationBearer $token")

  /**
   * Set request cookies.
   *
   * @param entries cookie names and values
   * @return HTTP properties
   */
  def cookies(entries: (String, String)*): Http[Base] =
    cookies(entries, headerCookie)

  /**
   * Add message header.
   *
   * @param name header name
   * @param value header value
   * @return HTTP properties
   */
  def header(name: String, value: String): Http[Base] = header(name, value, false)

  /**
   * Add or replace message header.
   *
   * @param name header name
   * @param value header value
   * @param replace replace all existing headers with the specied name
   * @return HTTP properties
   */
  def header(name: String, value: String, replace: Boolean): Http[Base] = {
    val originalHeaders = if (replace) headers.filter(_._1 != name) else headers
    copy(headers = originalHeaders :+ (name -> value))
  }

  /**
   * Add message headers.
   *
   * @param entries header names and values
   * @return HTTP properties
   */
  def headers(entries: (String, String)*): Http[Base] =
    headers(entries, false)

  /**
   * Add or replace message headers.
   *
   * @param entries header names and values
   * @param replace replace all existing headers with specified names
   * @return HTTP properties
   */
  def headers(entries: Iterable[(String, String)], replace: Boolean): Http[Base] = {
    val entryNames = entries.map { case (name, _) => name }.toSet
    val originalHeaders =
      if (replace) headers.filter { case (name, _) => !entryNames.contains(name) }
      else headers
    copy(headers = originalHeaders ++ entries)
  }

  /**
   * Add URL query parameter.
   *
   * @param name parameter name
   * @param value parameter value
   * @return HTTP properties
   */
  def parameter(name: String, value: String): Http[Base] = parameter(name, value, false)

  /**
   * Add or replace URL query parameter.
   *
   * @param name query parameter name
   * @param value query parameter value
   * @param replace replace all existing query parameters with the specied name
   * @return HTTP properties
   */
  def parameter(name: String, value: String, replace: Boolean): Http[Base] = {
    val originalParameters =
      if (replace) {
        parameters.filter(_._1 != name)
      } else parameters
    copy(parameters = originalParameters :+ (name -> value))
  }

  /**
   * Add URL query parameters.
   *
   * @param entries query parameter names and values
   * @return HTTP properties
   */
  def parameters(entries: (String, String)*): Http[Base] =
    parameters(entries, false)

  /**
   * Add or replace URL query parameters.
   *
   * @param entries query parameter names and values
   * @param replace replace all existing query parameters with specified names
   * @return HTTP properties
   */
  def parameters(entries: Iterable[(String, String)], replace: Boolean): Http[Base] = {
    val entryNames = entries.map { case (name, _) => name }.toSet
    val originalParameters =
      if (replace) {
        parameters.filter { case (name, _) => !entryNames.contains(name) }
      } else parameters
    copy(parameters = originalParameters ++ entries)
  }

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP properties
   */
  def proxyAuthBasic(user: String, password: String): Http[Base] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def proxyAuthBasic(token: String): Http[Base] =
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Proxy-Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP properties
   */
  def proxyAuthBearer(token: String): Http[Base] =
    header(headerProxyAuthorization, s"$headerAuthorizationBearer $token")

  /**
   * Set request URL query string.
   *
   * @param queryString URL uery string
   * @return HTTP properties
   */
  def query(queryString: String): Http[Base] = {
    val entries = queryString.replaceFirst("^\\?(.*)$", "$1")
    val parameters = entries.split("&").flatMap(_.split("=") match {
      case Array(name, value) if name.nonEmpty => Some((name, value))
      case Array(name) if name.nonEmpty => Some((name, ""))
      case _ => None
    }).toSeq
    copy(parameters = parameters)
  }

  /**
   * Set response cookies.
   *
   * @param entries cookie names and values
   * @return HTTP properties
   */
  def setCookies(values: (String, String)*): Http[Base] =
    cookies(values, headerSetCookie)

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP properties
   */
  def url(url: String): Http[Base] = this.url(new URI(url))

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP properties
   */
  def url(url: URI): Http[Base] = {
    val http = copy(
      scheme = Option(url.getScheme),
      authority = Option(url.getAuthority),
      path = Option(url.getPath),
      fragment = Option(url.getFragment)
    )
    Option(url.getQuery).map(http.query).getOrElse(http)
  }

  private def authorization(header: String, method: String): Option[String] =
    headers(header).find(_.trim.startsWith(method)).flatMap(_.split(" ") match {
      case Array(_, value) => Some(value)
      case _ => None
    })

  private def cookies(values: Iterable[(String, String)], headerName: String): Http[Base] = {
    val headerValue = (headers(headerName) ++ values.map { case (name, value) => s"$name=$value" }).mkString("; ")
    header(headerName, headerValue, true)
  }

  private def cookies(headerName: String): Map[String, Option[String]] =
    headers(headerName).flatMap { header =>
      header.split("=", 2).map(_.trim) match {
        case Array(name, value) => Some(name -> Some(value))
        case Array(name) => Some(name -> None)
        case _ => None
      }
    }.toMap
}

object Http {
  private val exceptionToStatusCode: Map[Class[_], Int] = Map[Class[_], Int](
    classOf[ParseErrorException] -> 400,
    classOf[InvalidRequestException] -> 400,
    classOf[FunctionNotFoundException] -> 501,
    classOf[IllegalArgumentException] -> 400,
    classOf[InternalErrorException] -> 500,
    classOf[ServerErrorException] -> 500,
    classOf[IOException] -> 500
  ).withDefaultValue(500)

  /**
   * Maps an exception to a corresponding default HTTP status code.
   *
   * @param exception exception class
   * @return HTTP status code
   */
  def defaultExceptionToStatusCode(exception: Throwable): Int =
    exceptionToStatusCode(exception.getClass)
}
