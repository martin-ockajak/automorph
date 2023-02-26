package automorph

import automorph.RpcException.InvalidResponseException
import automorph.client.meta.ClientMeta
import automorph.client.{ProtocolClientBuilder, RemoteMessage, TransportClientBuilder}
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.Extensions.{EffectOps, TryOps}
import automorph.util.Random
import java.io.InputStream
import scala.collection.immutable.ListMap
import scala.util.Try

/**
 * RPC client.
 *
 * The client can be used to perform type-safe remote API calls or send one-way messages.
 *
 * Remote APIs can be invoked statically using transparent proxy instances automatically derived from specified API
 * traits or dynamically by supplying the required type information on invocation.
 *
 * @constructor
 *   Creates a RPC client with specified protocol and transport plugins providing corresponding message context type.
 * @param protocol
 *   RPC protocol plugin
 * @param transport
 *   client message transport plugin
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   message context type
 */
final case class Client[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  protocol: RpcProtocol[Node, Codec, Context],
  transport: ClientMessageTransport[Effect, Context],
) extends ClientMeta[Node, Codec, Effect, Context] with Logging {

  protected val system: EffectSystem[Effect] = transport.system
  implicit private val givenSystem: EffectSystem[Effect] = transport.system

  /**
   * Creates an one-way remote API function message proxy.
   *
   * The remote function name and arguments are used to send an RPC request without expecting to receive a response.
   *
   * @param function
   *   remote function name
   * @return
   *   specified remote function one-way message proxy
   * @throws RpcException
   *   on RPC error
   */
  def message(function: String): RemoteMessage[Node, Codec, Effect, Context] =
    RemoteMessage(function, protocol.codec, sendMessage)

  /**
   * Closes this client freeing the underlying resources.
   *
   * @return
   *   nothing
   */
  def close(): Effect[Unit] =
    transport.close()

  /**
   * Creates a default request context.
   *
   * @return
   *   request context
   */
  def defaultContext: Context =
    transport.defaultContext

  /**
   * Messages a remote API function using specified arguments.
   *
   * Optional request context is used as a last remote function argument.
   *
   * @param function
   *   remote function name
   * @param arguments
   *   named arguments
   * @param requestContext
   *   request context
   * @return
   *   nothing
   */
  private def sendMessage(
    function: String,
    arguments: Seq[(String, Node)],
    requestContext: Option[Context],
  ): Effect[Unit] = {
    // Create request
    val requestId = Random.id
    protocol.createRequest(
      function,
      arguments,
      responseRequired = false,
      requestContext.getOrElse(defaultContext),
      requestId,
    ).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest =>
        system.successful(rpcRequest).flatMap { request =>
          lazy val requestProperties = rpcRequest.message.properties + (LogProperties.requestId -> requestId)
          lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.messageBody -> _)
          logger.trace(s"Sending ${protocol.name} request", allProperties)
          transport.message(request.message.body, request.context, requestId, protocol.codec.mediaType)
        },
    )
  }

  /**
   * Calls a remote API function using specified arguments.
   *
   * Optional request context is used as a last remote function argument.
   *
   * @param function
   *   remote function name
   * @param arguments
   *   named arguments
   * @param decodeResult
   *   decodes remote function result
   * @param requestContext
   *   request context
   * @tparam Result
   *   result type
   * @return
   *   result value
   */
  override def performCall[Result](
    function: String,
    arguments: Seq[(String, Node)],
    decodeResult: (Node, Context) => Result,
    requestContext: Option[Context],
  ): Effect[Result] = {
    // Create request
    val requestId = Random.id
    protocol.createRequest(
      function,
      arguments,
      responseRequired = true,
      requestContext.getOrElse(defaultContext),
      requestId,
    ).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest =>
        system.successful(rpcRequest).flatMap { request =>
          lazy val requestProperties = ListMap(LogProperties.requestId -> requestId) ++ rpcRequest.message.properties
          lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.messageBody -> _)
          logger.trace(s"Sending ${protocol.name} request", allProperties)
          transport.call(request.message.body, request.context, requestId, protocol.codec.mediaType).flatMap {
            case (responseBody, responseContext) =>
              // Process response
              processResponse[Result](responseBody, responseContext, requestProperties, decodeResult)
          }
        },
    )
  }

  /**
   * Processes an remote function call response.
   *
   * @param responseBody
   *   response message body
   * @param responseContext
   *   response context
   * @param requestProperties
   *   request properties
   * @param decodeResult
   *   decodes remote function call result
   * @tparam R
   *   result type
   * @return
   *   result value
   */
  private def processResponse[R](
    responseBody: InputStream,
    responseContext: Context,
    requestProperties: => Map[String, String],
    decodeResult: (Node, Context) => R,
  ): Effect[R] =
    // Parse response
    protocol.parseResponse(responseBody, responseContext).fold(
      error => raiseError(error.exception, requestProperties),
      rpcResponse => {
        lazy val allProperties = requestProperties ++ rpcResponse.message.properties ++
          rpcResponse.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Received ${protocol.name} response", allProperties)
        rpcResponse.result.pureFold(
          // Raise error
          error => raiseError(error, requestProperties),
          // Decode result
          result =>
            Try(decodeResult(result, responseContext)).pureFold(
              error => raiseError(InvalidResponseException("Malformed result", error), requestProperties),
              result => {
                logger.info(s"Performed ${protocol.name} request", requestProperties)
                system.successful(result)
              },
            ),
        )
      },
    )

  /**
   * Creates an error effect from an exception.
   *
   * @param error
   *   exception
   * @param properties
   *   message properties
   * @tparam T
   *   effectful value type
   * @return
   *   error value
   */
  private def raiseError[T](error: Throwable, properties: Map[String, String]): Effect[T] = {
    logger.error(s"Failed to perform ${protocol.name} request", error, properties)
    system.failed(error)
  }

  override def toString: String = {
    val plugins = Map[String, Any]("transport" -> transport, "protocol" -> protocol).map { case (name, plugin) =>
      s"$name = ${plugin.getClass.getName}"
    }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }
}

object Client {

  /**
   * Creates an RPC client builder with specified RPC protocol plugin.
   *
   * @param protocol
   *   RPC protocol plugin
   * @tparam Node
   *   message node type
   * @tparam Codec
   *   message codec plugin type
   * @tparam Context
   *   message context type
   * @return
   *   RPC client builder
   */
  def protocol[Node, Codec <: MessageCodec[Node], Context](
    protocol: RpcProtocol[Node, Codec, Context]
  ): ProtocolClientBuilder[Node, Codec, Context] =
    ProtocolClientBuilder(protocol)

  /**
   * Creates an RPC client builder with specified effect transport plugin.
   *
   * @param transport
   *   message transport plugin
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   message context type
   * @return
   *   RPC client builder
   */
  def transport[Effect[_], Context](
    transport: ClientMessageTransport[Effect, Context]
  ): TransportClientBuilder[Effect, Context] =
    TransportClientBuilder(transport)
}
