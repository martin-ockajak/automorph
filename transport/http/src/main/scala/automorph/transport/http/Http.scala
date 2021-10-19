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
 * @param userInfo request URL authority user information
 * @param host request URL authority host
 * @param port request URL authority port
 * @param path request URL path
 * @param fragment request URL fragment
 * @param headers request headers
 * @param followRedirects automatically follow redirects if true
 * @param readTimeout response read timeout
 * @param base base properties defined by the specific message transport plugin
 * @tparam Base specific message transport plugin base properties type
 */
final case class Http[Base](
  method: Option[String] = None,
  scheme: Option[String] = None,
  userInfo: Option[String] = None,
  host: Option[String] = None,
  port: Option[Int] = None,
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

  /** Request URL. */
  def url: Option[URI] = (scheme, authority, path, query, fragment) match {
    case (Some(scheme), Some(authority), Some(path), query, fragment) =>
      Some(new URI(scheme, authority, path, query.orNull, fragment.orNull))
    case _ => None
  }

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP message properties
   */
  def url(url: URI): Http[Base] = {
    val http = copy(
      scheme = Option(url.getScheme),
      userInfo = Option(url.getUserInfo),
      host = Option(url.getHost),
      port = Option.when(url.getPort >= 0)(url.getPort),
      path = Option(url.getPath),
      fragment = Option(url.getFragment)
    )
    Option(url.getQuery).map(http.query).getOrElse(http)
  }

  /**
   * Set request URL.
   *
   * @param url URL
   * @return HTTP message properties
   */
  def url(url: String): Http[Base] = this.url(new URI(url))

  /**
   * Set request URL scheme.
   *
   * @param scheme URL scheme
   * @return HTTP message properties
   */
  def scheme(scheme: String): Http[Base] = {
    copy(scheme = Some(scheme))
  }

  /** Request URL authority. */
  def authority: Option[String] = path.map { path =>
    val userInfoText = userInfo.map(userInfo => s"$userInfo@").getOrElse("")
    val portText = port.map(port => s":$port").getOrElse("")
    s"$userInfoText$path$portText"
  }

  /**
   * Set request URL authority.
   *
   * @param authority URL authority
   * @return HTTP message properties
   */
  def authority(authority: String): Http[Base] = {
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
   * @return HTTP message properties
   */
  def userInfo(userInfo: String): Http[Base] = {
    copy(userInfo = Some(userInfo))
  }

  /**
   * Set request URL host.
   *
   * @param host URL host
   * @return HTTP message properties
   */
  def host(host: String): Http[Base] = {
    copy(host = Some(host))
  }

  /**
   * Set request URL port.
   *
   * @param port URL port
   * @return HTTP message properties
   */
  def port(port: Int): Http[Base] = {
    copy(port = Some(port))
  }

  /**
   * Set request URL user information.
   *
   * @param path URL userinfo
   * @return HTTP message properties
   */
  def path(path: String): Http[Base] = {
    copy(path = Some(path))
  }

  /**
   * Set request URL fragment.
   *
   * @param fragment URL fragment
   * @return HTTP message properties
   */
  def fragment(fragment: String): Http[Base] = {
    copy(fragment = Some(fragment))
  }

  /** Request URL query. */
  def query: Option[String] = parameters match {
    case Seq() => None
    case _ => Some(s"?${parameters.map { case (name, value) => s"$name=$value" }.mkString("&")}")
  }

  /**
   * Set request URL query string.
   *
   * @param queryString URL query string
   * @return HTTP message properties
   */
  def query(queryString: String): Http[Base] = {
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
   * @return HTTP message properties
   */
  def parameter(name: String, value: String): Http[Base] = parameter(name, value, false)

  /**
   * Add or replace URL query parameter.
   *
   * @param name query parameter name
   * @param value query parameter value
   * @param replace replace all existing query parameters with the specied name
   * @return HTTP message properties
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
   * @return HTTP message properties
   */
  def parameters(entries: (String, String)*): Http[Base] =
    parameters(entries, false)

  /**
   * Add or replace URL query parameters.
   *
   * @param entries query parameter names and values
   * @param replace replace all existing query parameters with specified names
   * @return HTTP message properties
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
   * Add message header.
   *
   * @param name header name
   * @param value header value
   * @return HTTP message properties
   */
  def header(name: String, value: String): Http[Base] = header(name, value, false)

  /**
   * Add or replace message header.
   *
   * @param name header name
   * @param value header value
   * @param replace replace all existing headers with the specied name
   * @return HTTP message properties
   */
  def header(name: String, value: String, replace: Boolean): Http[Base] = {
    val originalHeaders = if (replace) headers.filter(_._1 != name) else headers
    copy(headers = originalHeaders :+ (name -> value))
  }

  /**
   * Add message headers.
   *
   * @param entries header names and values
   * @return HTTP message properties
   */
  def headers(entries: (String, String)*): Http[Base] =
    headers(entries, false)

  /**
   * Add or replace message headers.
   *
   * @param entries header names and values
   * @param replace replace all existing headers with specified names
   * @return HTTP message properties
   */
  def headers(entries: Iterable[(String, String)], replace: Boolean): Http[Base] = {
    val entryNames = entries.map { case (name, _) => name }.toSet
    val originalHeaders =
      if (replace) headers.filter { case (name, _) => !entryNames.contains(name) }
      else headers
    copy(headers = originalHeaders ++ entries)
  }

  /** `Content-Type` header value. */
  def contentType: Option[String] = header(headerContentType)

  /** `Content-Length` header value. */
  def contentLength: Option[String] = header(headerContentLength)

  /** Cookie names and values. */
  def cookies: Map[String, Option[String]] = cookies(headerCookie)

  /**
   * Cookie value.
   *
   * @param name cookie name
   * @return cookie value
   */
  def cookie(name: String): Option[String] = cookies.get(name).flatten

  /**
   * Set request cookies.
   *
   * @param entries cookie names and values
   * @return HTTP message properties
   */
  def cookies(entries: (String, String)*): Http[Base] =
    cookies(entries, headerCookie)

  /** Set-Cookie names and values. */
  def setCookies: Map[String, Option[String]] = cookies(headerSetCookie)

  /**
   * Set response cookies.
   *
   * @param entries cookie names and values
   * @return HTTP message properties
   */
  def setCookies(values: (String, String)*): Http[Base] =
    cookies(values, headerSetCookie)

  /** `Authorization` header value. */
  def authorization: Option[String] = header(headerAuthorization)

  /** `Authorization: Basic` header value. */
  def authorizationBasic: Option[String] = authorization(headerAuthorization, headerAuthorizationBasic)

  /** `Authorization: Bearer` header value. */
  def authorizationBearer: Option[String] = authorization(headerAuthorization, headerAuthorizationBearer)

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP message properties
   */
  def authorizationBasic(user: String, password: String): Http[Base] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP message properties
   */
  def authorizationBasic(token: String): Http[Base] =
    header(headerAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP message properties
   */
  def authorizationBearer(token: String): Http[Base] =
    header(headerAuthorization, s"$headerAuthorizationBearer $token")

  /** `Proxy-Authorization` header value. */
  def proxyAuthorization: Option[String] = header(headerProxyAuthorization)

  /** `Proxy-Authorization: Basic` header value. */
  def proxyAuthorizationBasic: Option[String] = authorization(headerProxyAuthorization, headerAuthorizationBasic)

  /** `Proxy-Authorization: Bearer` header value. */
  def proxyAuthorizationBearer: Option[String] = authorization(headerProxyAuthorization, headerAuthorizationBearer)

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param user user
   * @param password password
   * @return HTTP message properties
   */
  def proxyAuthBasic(user: String, password: String): Http[Base] = {
    val value = new String(Base64.getEncoder.encode(s"$user:$password".getBytes(charset)), charset)
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $value")
  }

  /**
   * Set `Proxy-Authorization: Basic` header value.
   *
   * @param token authentication token
   * @return HTTP message properties
   */
  def proxyAuthBasic(token: String): Http[Base] =
    header(headerProxyAuthorization, s"$headerAuthorizationBasic $token")

  /**
   * Set `Proxy-Authorization: Bearer` header value.
   *
   * @param token authentication token
   * @return HTTP message properties
   */
  def proxyAuthBearer(token: String): Http[Base] =
    header(headerProxyAuthorization, s"$headerAuthorizationBearer $token")

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
