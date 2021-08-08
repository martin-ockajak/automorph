package automorph.client

import automorph.log.Logging
import automorph.protocol.RpcRequest
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.{ClientMessageTransport, EffectSystem, MessageFormat, RpcProtocol}
import automorph.util.Extensions.TryOps
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * RPC client core logic.
 *
 * @param format message format plugin
 * @param system effect system plugin
 * @param transport message transport plugin
 * @param protocol RPC protocol
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] case class ClientCore[Node, Format <: MessageFormat[Node], Effect[_], Context](
  format: Format,
  private val system: EffectSystem[Effect],
  private val transport: ClientMessageTransport[Effect, Context],
  private val protocol: RpcProtocol
) extends Logging {

  private val bodyProperty = "Body"

  /**
   * Performs a method call using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param method method name
   * @param argumentNames argument names
   * @param encodedArguments method argument nodes
   * @param decodeResult result node decoding function
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def call[R](
    method: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    decodeResult: Node => R,
    context: Option[Context]
  ): Effect[R] =
    // Create request
    protocol.createRequest(method, argumentNames, encodedArguments, true, format).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest => {
        lazy val properties = rpcRequest.message.properties ++ rpcRequest.message.text.map(bodyProperty -> _())
        logger.trace(s"Sending ${protocol.name} request", properties)
        system.flatMap(
          system.pure(rpcRequest),
          (request: RpcRequest[Node, _]) =>
            system.flatMap(
              transport.call(request.message.body, format.mediaType, context),
              // Process response
              rawResponse => processResponse[R](rawResponse, request.message.properties, decodeResult)
            )
        )
      }
    )

  /**
   * Performs a method notification using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param method method name
   * @param argumentNames argument names
   * @param encodedArguments method argument nodes
   * @param context request context
   * @return nothing
   */
  private[automorph] def notify(
    method: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    context: Option[Context]
  ): Effect[Unit] =
    // Create request
    protocol.createRequest(method, argumentNames, encodedArguments, false, format).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest =>
        system.flatMap(
          system.pure(rpcRequest),
          (request: RpcRequest[Node, _]) => transport.notify(request.message.body, format.mediaType, context)
        )
    )

  /**
   * Processes a method call response.
   *
   * @param rawResponse raw response
   * @param requestProperties request properties
   * @param decodeResult result decoding function
   * @tparam R result type
   * @return result value
   */
  private def processResponse[R](
    rawResponse: ArraySeq.ofByte,
    requestProperties: Map[String, String],
    decodeResult: Node => R
  ): Effect[R] =
    // Parse response
    protocol.parseResponse(rawResponse, format).fold(
      error => raiseError(error.exception, requestProperties),
      rpcResponse => {
        lazy val properties = requestProperties ++ rpcResponse.message.properties
        logger.trace(s"Received ${protocol.name} response", properties ++ rpcResponse.message.text.map(bodyProperty -> _()))
        rpcResponse.result.pureFold(
          // Raise error
          error => raiseError(error, requestProperties ++ properties),
          // Decode result
          result =>
            Try(decodeResult(result)).pureFold(
              error => raiseError(InvalidResponseException("Invalid result", error), properties),
              result => {
                logger.info(s"Performed ${protocol.name} request", properties)
                system.pure(result)
              }
            )
        )
      }
    )

  /**
   * Creates an error effect from an exception.
   *
   * @param error exception
   * @param properties message properties
   * @tparam T effectful value type
   * @return error value
   */
  private def raiseError[T](error: Throwable, properties: Map[String, String]): Effect[T] = {
    logger.error(s"Failed to perform ${protocol.name} request", error, properties)
    system.failed(error)
  }
}
