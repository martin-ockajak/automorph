package automorph.client

import automorph.Client
import automorph.log.LogProperties
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.protocol.RpcRequest
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.util.Extensions.TryOps
import automorph.util.Random
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.util.Try

/**
 * RPC client core logic.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait ClientCore[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Client[Node, Codec, Effect, Context] =>

  /** This client type. */
  type ThisClient = Client[Node, Codec, Effect, Context]

  /**
   * Performs an RPC function call using specified arguments.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param functionName RPC function name
   * @param argumentNames argument names
   * @param encodedArguments function argument nodes
   * @param decodeResult result node decoding function
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def call[R](
    functionName: String,
    argumentNames: Seq[String],
    encodedArguments: Seq[Node],
    decodeResult: Node => R,
    context: Option[Context]
  ): Effect[R] =
    // Create request
    val requestId = Random.id
    protocol.createRequest(functionName, Some(argumentNames), encodedArguments, true, requestId).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest => {
        system.flatMap(
          system.pure(rpcRequest),
          (request: RpcRequest[Node, _]) => {
            val rawRequest = request.message.body
            lazy val requestProperties = ListMap(LogProperties.requestId -> requestId) ++
              rpcRequest.message.properties + (LogProperties.size -> rawRequest.length.toString)
            lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.body -> _)
            logger.trace(s"Sending ${protocol.name} request", allProperties)
            system.flatMap(
              transport.call(rawRequest, requestId, protocol.codec.mediaType, context),
              // Process response
              rawResponse => processResponse[R](rawResponse, requestProperties, decodeResult)
            )
          }
        )
      }
    )

  /**
   * Performs an RPC function notification using specified arguments.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param functionName RPC function name
   * @param argumentNames argument names
   * @param encodedArguments function argument nodes
   * @param context request context
   * @return nothing
   */
  private[automorph] def notify(
    functionName: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    context: Option[Context]
  ): Effect[Unit] =
    // Create request
    val requestId = Random.id
    protocol.createRequest(functionName, argumentNames, encodedArguments, false, requestId).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest =>
        system.flatMap(
          system.pure(rpcRequest),
          (request: RpcRequest[Node, _]) => {
            val rawRequest = request.message.body
            lazy val requestProperties = rpcRequest.message.properties ++ Map(
              LogProperties.requestId -> requestId,
              LogProperties.size -> rawRequest.length.toString
            )
            lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.body -> _)
            logger.trace(s"Sending ${protocol.name} request", allProperties)
            transport.notify(request.message.body, requestId, protocol.codec.mediaType, context)
          }
        )
    )

  /**
   * Processes an RPC function call response.
   *
   * @param rawResponse raw response
   * @param requestProperties request properties
   * @param decodeResult result decoding function
   * @tparam R result type
   * @return result value
   */
  private def processResponse[R](
    rawResponse: ArraySeq.ofByte,
    requestProperties: => Map[String, String],
    decodeResult: Node => R
  ): Effect[R] =
    // Parse response
    protocol.parseResponse(rawResponse).fold(
      error => raiseError(error.exception, requestProperties),
      rpcResponse => {
        lazy val allProperties = requestProperties ++ rpcResponse.message.properties +
          (LogProperties.size -> rawResponse.length.toString) ++ rpcResponse.message.text.map(LogProperties.body -> _)
        logger.trace(s"Received ${protocol.name} response", allProperties)
        rpcResponse.result.pureFold(
          // Raise error
          error => raiseError(error, requestProperties),
          // Decode result
          result =>
            Try(decodeResult(result)).pureFold(
              error => raiseError(InvalidResponseException("Malformed result", error), requestProperties),
              result => {
                logger.info(s"Performed ${protocol.name} request", requestProperties)
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
