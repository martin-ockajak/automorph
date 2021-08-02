package automorph.client

import automorph.log.Logging
import automorph.protocol.jsonrpc.ErrorType.{InvalidResponseException, ParseErrorException}
import automorph.protocol.jsonrpc.{Request, Response}
import automorph.spi.Message.Params
import automorph.spi.{ClientMessageTransport, EffectSystem, Message, MessageFormat}
import automorph.util.Extensions.TryOps
import automorph.util.MessageId
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC client core logic.
 *
 * @param format message format plugin
 * @param system effect system plugin
 * @param transport message transport plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] case class ClientCore[
  Node,
  Format <: MessageFormat[Node],
  Effect[_],
  Context
] private[automorph] (
  format: Format,
  private val system: EffectSystem[Effect],
  private val transport: ClientMessageTransport[Effect, Context],
  private val errorToException: (Int, String) => Throwable
) extends Logging {

  /**
   * Performs a method call using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param argumentNames argument names
   * @param encodedArguments method argument nodes
   * @param decodeResultNode result node decoding function
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  def call[R](
    methodName: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    decodeResultNode: Node => R,
    context: Option[Context]
  ): Effect[R] = {
    val argumentNodes = createArgumentNodes(argumentNames, encodedArguments)
    val id = Some(Right[BigDecimal, String](MessageId.next))
    val formedRequest = Request(id, methodName, argumentNodes).formed
    logger.debug(s"Performing JSON-RPC request", formedRequest.properties)
    system.flatMap(
      // Serialize request
      serialize(formedRequest),
      (rawRequest: ArraySeq.ofByte) =>
        // Send request
        system.flatMap(
          transport.call(rawRequest, format.mediaType, context),
          // Process response
          rawResponse => processResponse[R](rawResponse, formedRequest, decodeResultNode)
        )
    )
  }

  /**
   * Performs a method notification using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param argumentNames argument names
   * @param encodedArguments method argument nodes
   * @param context request context
   * @return nothing
   */
  private[automorph] def notify(
    methodName: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    context: Option[Context]
  ): Effect[Unit] = {
    val argumentNodes = createArgumentNodes(argumentNames, encodedArguments)
    val formedRequest = Request(None, methodName, argumentNodes).formed
    system.flatMap(
      // Serialize request
      serialize(formedRequest),
      // Send request
      (rawRequest: ArraySeq.ofByte) => transport.notify(rawRequest, format.mediaType, context)
    )
  }

  /**
   * Creates method invocation argument nodes.
   *
   * @param argumentNames argument names
   * @param encodedArguments encoded arguments
   * @return argument nodes
   */
  private def createArgumentNodes(argumentNames: Option[Seq[String]], encodedArguments: Seq[Node]): Params[Node] =
    argumentNames.filter(_.size >= encodedArguments.size).map { names =>
      Right(names.zip(encodedArguments).toMap)
    }.getOrElse(Left(encodedArguments.toList))

  /**
   * Processes a method call response.
   *
   * @param rawResponse raw response
   * @param formedRequest formed request
   * @param decodeResult result decoding function
   * @tparam R result type
   * @return result value
   */
  private def processResponse[R](
    rawResponse: ArraySeq.ofByte,
    formedRequest: Message[Node],
    decodeResult: Node => R
  ): Effect[R] =
    // Deserialize response
    Try(format.deserialize(rawResponse)).pureFold(
      error => raiseError(ParseErrorException("Invalid response format", error), formedRequest),
      formedResponse => {
        // Validate response
        logger.trace(s"Received JSON-RPC response:\n${format.format(formedResponse)}")
        Try(Response(formedResponse)).pureFold(
          error => raiseError(error, formedRequest),
          validResponse =>
            validResponse.error.fold(
              validResponse.result.fold {
                raiseError(InvalidResponseException("Invalid result", None.orNull), formedRequest)
              } { result =>
                // Decode result
                Try(decodeResult(result)).pureFold(
                  error => raiseError(InvalidResponseException("Invalid result", error), formedRequest),
                  result => {
                    logger.info(s"Performed JSON-RPC request", formedRequest.properties)
                    system.pure(result)
                  }
                )
              }
            ) { error =>
              raiseError(errorToException(error.code, error.message), formedRequest)
            }
        )
      }
    )

  /**
   * Serializes a JSON-RPC message.
   *
   * @param formedRequest formed request
   * @return serialized response
   */
  private def serialize(formedRequest: Message[Node]): Effect[ArraySeq.ofByte] = {
    logger.trace(s"Sending JSON-RPC request:\n${format.format(formedRequest)}")
    Try(format.serialize(formedRequest)).pureFold(
      error => raiseError(ParseErrorException("Invalid request format", error), formedRequest),
      message => system.pure(message)
    )
  }

  /**
   * Creates an error effect from an exception.
   *
   * @param error exception
   * @param requestMessage request message
   * @tparam T effectful value type
   * @return error value
   */
  private def raiseError[T](error: Throwable, requestMessage: Message[Node]): Effect[T] = {
    logger.error(s"Failed to perform JSON-RPC request", error, requestMessage.properties)
    system.failed(error)
  }
}
