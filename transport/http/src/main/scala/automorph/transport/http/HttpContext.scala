package automorph.transport.http

import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, ParseErrorException, ServerErrorException}
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException}
import automorph.util.Extensions.{ByteArrayOps, StringOps}
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import scala.concurrent.duration.Duration

/**
 * HTTP transport message context.
 *
 * Message transport plugins must use message context properties in the descending order of priority by source:
 * - This context
 * - Transport plugin context (this.transport)
 * - Default values
 *
 * @see [[https://datatracker.ietf.org/doc/html/rfc7232 HTTP specification]]
 * @param scheme request URL scheme
 * @param userInfo request URL authority user information
 * @param host request URL authority host
 * @param port request URL authority port
 * @param path request URL path
 * @param fragment request URL fragment
 * @param headers request or response headers
 * @param method request method
 * @param statusCode response status code
 * @param followRedirects automatically follow redirects if true
 * @param timeout response timeout
 * @param transport specific transport plugin message context
 * @tparam Transport specific transport plugin message context type
 */
final case class HttpContext[Transport](
  scheme: Option[String] = None,
  userInfo: Option[String] = None,
  host: Option[String] = None,
  port: Option[Int] = None,
  path: Option[String] = None,
  parameters: Seq[(String, String)] = Seq.empty,
  fragment: Option[String] = None,
  headers: Seq[(String, String)] = Seq.empty,
  method: Option[HttpMethod] = None,
  statusCode: Option[Int] = None,
  followRedirects: Option[Boolean] = None,
  timeout: Option[Duration] = None,
  transport: Option[Transport] = None
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

  /** Request URL. */
  def url: Option[URI] =
    (scheme, authority, path, query, fragment) match {
      case (Some(scheme), Some(authority), Some(path), query, fragment) =>
        Some(new URI(scheme, authority, path, query.orNull, fragment.orNull))
      case _ => None
    }

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP message context
   */
  def url(url: URI): HttpContext[Transport] = {
    val httpContext = copy(
      scheme = Option(url.getScheme),
      userInfo = Option(url.getUserInfo),
      host = Option(url.getHost),
      port = Option.when(url.getPort >= 0)(url.getPort),
      path = Option(url.getPath),
      fragment = Option(url.getFragment)
    )
    Option(url.getQuery).map(httpContext.query).getOrElse(httpContext)
  }

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP message context
   */
  def url(url: String): HttpContext[Transport] =
    this.url(new URI(url))

  /**
   * Set request URL scheme.
   *
   * @param scheme URL scheme
   * @return HTTP message context
   */
  def scheme(scheme: String): HttpContext[Transport] =
    copy(scheme = Some(scheme))

  /** Request URL authority. */
  def authority: Option[String] =
    host.map { host =>
      val userInfoText = userInfo.map(userInfo => s"$userInfo@").getOrElse("")
      val portText = port.map(port => s":$port").getOrElse("")
      s"$userInfoText$host$portText"
    }

  /**
   * Set request URL authority.
   *
   * @param authority URL authority
   * @return HTTP message context
   */
  def authority(authority: String): HttpContext[Transport] = {
    val (userInfo, endpoint) = authority.split("@", 2) match {
      case Array(userInfo, endpoint) => Some(userInfo) -> Some(endpoint)
      case Array(endpoint) => None -> Some(endpoint)
      case _ => None -> None
    }
    val (host, port) = endpoint.map(_.split(":", 2) match {
      case Array(host, port) => Some(host) -> Some(port.toInt)
      case Array(host) => Some(host) -> None
      case _ => None -> None
    }).getOrElse(None -> None)
    copy(userInfo = userInfo, host = host, port = port)
  }

  /**
   * Set request URL user information.
   *
   * @param userInfo URL user information
   * @return HTTP message context
   */
  def userInfo(userInfo: String): HttpContext[Transport] =
    copy(userInfo = Some(userInfo))

  /**
   * Set request URL host.
   *
   * @param host URL host
   * @return HTTP message context
   */
  def host(host: String): HttpContext[Transport] =
    copy(host = Some(host))

  /**
   * Set request URL port.
   *
   * @param port URL port
   * @return HTTP message context
   */
  def port(port: Int): HttpContext[Transport] =
    copy(port = Some(port))

  /**
   * Set request URL user information.
   *
   * @param path URL userinfo
   * @return HTTP message context
   */
  def path(path: String): HttpContext[Transport] =
    copy(path = Some(path))

  /**
   * Set request URL fragment.
   *
   * @param fragment URL fragment
   * @return HTTP message context
   */
  def fragment(fragment: String): HttpContext[Transport] =
    copy(fragment = Some(fragment))

  /** Request URL query. */
  def query: Option[String] =
    parameters match {
      case Seq() => None
      case _ => Some(s"${parameters.map { case (name, value) => s"$name=$value" }.mkString("&")}")
    }

  /**
   * Set request URL query string.
   *
   * @param queryString URL query string
   * @return HTTP message context
   */
  def query(queryString: String): HttpContext[Transport] = {
    val entries = queryString.replaceFirst("^\\?(.*)$", "$1")
    val parameters = entries.split("&").flatMap(_.split("=", 2) match {
      case Array(name, value) if name.nonEmpty => Some((name, value))
      case Array(name) if name.nonEmpty => Some((name, ""))
      case _ => None
    }).toSeq
    copy(parameters = parameters)
  }

  /**
   * Add URL query parameter.
   *
   * @param name parameter name
   * @param value parameter value
   * @return HTTP message context
   */
  def parameter(name: String, value: String): HttpContext[Transport] =
    parameter(name, value, replace = false)

  /**
   * Add or replace URL query parameter.
   *
   * @param name query parameter name
   * @param value query parameter value
   * @param replace replace all existing query parameters with the specied name
   * @return HTTP message context
   */
  def parameter(name: String, value: String, replace: Boolean): HttpContext[Transport] = {
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
   * @return HTTP message context
   */
  def parameters(entries: (String, String)*): HttpContext[Transport] =
    parameters(entries, replace = false)

  /**
   * Add or replace URL query parameters.
   *
   * @param entries query parameter names and values
   * @param replace replace all existing query parameters with specified names
   * @return HTTP message context
   */
  def parameters(entries: Iterable[(String, String)], replace: Boolean): HttpContext[Transport] = {
    val entryNames = entries.map { case (name, _) => name }.toSet
    val originalParameters =
      if (replace) {
        parameters.filter { case (name, _) => !entryNames.contains(name) }
      } else parameters
    copy(parameters = originalParameters ++ entries)
  }

  /**
   * First URL query parameter value.
   *
   * @param name query parameter name
   * @return first query parameter value
   */
  def parameter(name: String): Option[String] =
    parameters.find(_._1 == name).map(_._2)

  /**
   * URL query parameter values.
   *
   * @param name query parameter name
   * @return query parameter values
   */
  def parameters(name: String): Seq[String] =
    parameters.filter(_._1 == name).map(_._2)

  /**
   * First header value.
   *
   * @param name header name
   * @return first header value
   */
  def header(name: String): Option[String] =
    headers.find(_._1 == name).map(_._2)

  /**
   * Header values.
   *
   * @param name header name
   * @return header values
   */
  def headers(name: String): Seq[String] =
    headers.filter(_._1 == name).map(_._2)

  /**
   * Add message header.
   *
   * @param name header name
   * @param value header value
   * @return HTTP message context
   */
  def header(name: String, value: String): HttpContext[Transport] =
    header(name, value, replace = false)

  /**
   * Add or replace message header.
   *
   * @param name header name
   * @param value header value
   * @param replace replace all existing headers with the specied name
   * @return HTTP message context
   */
  def header(name: String, value: String, replace: Boolean): HttpContext[Transport] = {
    val originalHeaders = if (replace) headers.filter(_._1 != name) else headers
    copy(headers = originalHeaders :+ (name -> value))
  }

  /**
   * Add message headers.
   *
   * @param entries header names and values
   * @return HTTP message context
   */
  def headers(entries: (String, String)*): HttpContext[Transport] =
    headers(entries, replace = false)

  /**
   * Add or replace message headers.
   *
   * @param entries header names and values
   * @param replace replace all existing headers with specified names
   * @return HTTP message context
   */
  def headers(entries: Iterable[(String, String)], replace: Boolean): HttpContext[Transport] = {
    val entryNames = entries.map { case (name, _) => name }.toSet
    val originalHeaders =
      if (replace) headers.filter { case (name, _) => !entryNames.contains(name) }
      else headers
    copy(headers = originalHeaders ++ entries)
  }

  /**
   * Set request method.
   *
   * @param method request method
   * @return HTTP message context
   */
  def method(method: HttpMethod): HttpContext[Transport] =
    copy(method = Some(method))

  /**
   * Set response status code.
   *
   * @param statusCode status code
   * @return HTTP message context
   */
  def statusCode(statusCode: Int): HttpContext[Transport] =
    copy(statusCode = Some(statusCode))

  /** `Content-Type` header value. */
  def contentType: Option[String] =
    header(headerContentType)

  /** `Content-Length` header value. */
  def contentLength: Option[String] =
    header(headerContentLength)

  /** Cookie names and values. */
  def cookies: Map[String, Option[String]] =
    cookies(headerCookie)

  /**
   * Cookie value.
   *
   * @param name cookie name
   * @return cookie value
   */
  def cookie(name: String): Option[String] =
    cookies.get(name).flatten

  /**
   * Set request cookies.
   *
   * @param entries cookie names and values
   * @return HTTP message context
   */
  def cookies(entries: (String, String)*): HttpContext[Transport] =
    cookies(entries, headerCookie)

  /** Set-Cookie names and values. */
  def setCookies: Map[String, Option[String]] =
    cookies(headerSetCookie)

  /**
   * Set response cookies.
   *
   * @param entries cookie names and values
   * @return HTTP message context
   */
  def setCookies(entries: (String, String)*): HttpContext[Transport] =
    cookies(entries, headerSetCookie)

  /** `Authorization` header value. */
  def authorization: Option[String] =
    header(headerAuthorization)

  /** `Authorization: Basic` header value. */
  def authorizationBasic: Option[String] =
    authorization(headerAuthorization, headerAuthorizationBasic)

  /** `Authorization: Bearer` header value. */
  def authorizationBearer: Option[String] =
    authorization(headerAuthorization, headerAuthorizationBearer)

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP message context
   */
  def authorizationBasic(user: String, password: String): HttpContext[Transport] = {
    val value = Base64.getEncoder.encode(s"$user:$password".toArray).asString
    header(headerAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP message context
   */
  def authorizationBasic(token: String): HttpContext[Transport] =
    header(headerAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP message context
   */
  def authorizationBearer(token: String): HttpContext[Transport] =
    header(headerAuthorization, s"$headerAuthorizationBearer $token")

  /** `Proxy-Authorization` header value. */
  def proxyAuthorization: Option[String] =
    header(headerProxyAuthorization)

  /** `Proxy-Authorization: Basic` header value. */
  def proxyAuthorizationBasic: Option[String] =
    authorization(headerProxyAuthorization, headerAuthorizationBasic)

  /** `Proxy-Authorization: Bearer` header value. */
  def proxyAuthorizationBearer: Option[String] =
    authorization(headerProxyAuthorization, headerAuthorizationBearer)

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP message context
   */
  def proxyAuthBasic(user: String, password: String): HttpContext[Transport] = {
    val value = Base64.getEncoder.encode(s"$user:$password".toArray).asString
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP message context
   */
  def proxyAuthBasic(token: String): HttpContext[Transport] =
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Proxy-Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP message context
   */
  def proxyAuthBearer(token: String): HttpContext[Transport] =
    header(headerProxyAuthorization, s"$headerAuthorizationBearer $token")

  private[automorph] def overrideUrl(url: URI): URI = {
    val base = HttpContext().url(url)
    val scheme = this.scheme.map(base.scheme).getOrElse(base)
    val authority = this.authority.map(scheme.authority).getOrElse(scheme)
    val path = this.path.map(authority.path).getOrElse(authority)
    val fragment = this.fragment.map(path.fragment).getOrElse(path)
    val query = fragment.parameters(parameters*)
    query.url.getOrElse(url)
  }

  private def authorization(header: String, method: String): Option[String] =
    headers(header).find(_.trim.startsWith(method)).flatMap(_.split(" ") match {
      case Array(_, value) => Some(value)
      case _ => None
    })

  private def cookies(values: Iterable[(String, String)], headerName: String): HttpContext[Transport] = {
    val headerValue = (headers(headerName) ++ values.map { case (name, value) => s"$name=$value" }).mkString("; ")
    header(headerName, headerValue, replace = true)
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

object HttpContext {

  private val exceptionToStatusCode: Map[Class[?], Int] = Map[Class[?], Int](
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
