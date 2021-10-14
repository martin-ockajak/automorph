package automorph.client

import automorph.Client
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.protocol.RpcRequest
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.util.Extensions.TryOps
import automorph.util.Random
import scala.collection.immutable.ArraySeq
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

  /** Named RPC function proxy type. */
  type NamedFunction = NamedProxy[Node, Codec, Effect, Context]

  private val bodyProperty = "Body"
  private val requestIdProperty = "RequestId"

  /**
   * Creates a remote RPC function proxy with specified function name.
   *
   * @param functionName remote RPC function name
   * @return remote RPC function proxy with specified function name
   */
  def function(functionName: String): NamedFunction = NamedProxy(functionName, this, protocol.codec, Seq.empty, Seq.empty)

  /**
   * Creates default request context.
   *
   * @return request context
   */
  def context: Context = transport.defaultContext

  /**
   * Closes this client freeing the underlying resources.
   *
   * @return nothing
   */
  def close(): Effect[Unit] = transport.close()

  override def toString: String = {
    val plugins = Map(
      "transport" -> transport,
      "protocol" -> protocol
    ).map { case (name, plugin) => s"$name = ${plugin.getClass.getName}" }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }

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
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    decodeResult: Node => R,
    context: Option[Context]
  ): Effect[R] =
    // Create request
    protocol.createRequest(functionName, argumentNames, encodedArguments, true).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest => {
        val requestId = Random.id
        lazy val requestProperties = rpcRequest.message.properties + (requestIdProperty -> requestId)
        lazy val allProperties = rpcRequest.message.properties ++ rpcRequest.message.text.map(bodyProperty -> _)
        logger.trace(s"Sending ${protocol.name} request", allProperties)
        system.flatMap(
          system.pure(rpcRequest),
          (request: RpcRequest[Node, _]) =>
            system.flatMap(
              transport.call(request.message.body, protocol.codec.mediaType, context),
              // Process response
              rawResponse => processResponse[R](rawResponse, request.message.properties, decodeResult)
            )
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
    protocol.createRequest(functionName, argumentNames, encodedArguments, false).pureFold(
      error => system.failed(error),
      // Send request
      rpcRequest =>
        system.flatMap(
          system.pure(rpcRequest),
          (request: RpcRequest[Node, _]) => transport.notify(request.message.body, protocol.codec.mediaType, context)
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
    requestProperties: Map[String, String],
    decodeResult: Node => R
  ): Effect[R] =
    // Parse response
    protocol.parseResponse(rawResponse).fold(
      error => raiseError(error.exception, requestProperties),
      rpcResponse => {
        lazy val properties = requestProperties ++ rpcResponse.message.properties
        logger.trace(s"Received ${protocol.name} response", properties ++ rpcResponse.message.text.map(bodyProperty -> _))
        rpcResponse.result.pureFold(
          // Raise error
          error => raiseError(error, requestProperties ++ properties),
          // Decode result
          result =>
            Try(decodeResult(result)).pureFold(
              error => raiseError(InvalidResponseException("Malformed result", error), properties),
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
