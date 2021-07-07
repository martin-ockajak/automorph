package automorph.client

import automorph.log.Logging
import automorph.protocol.ErrorType.{InvalidResponseException, ParseErrorException}
import automorph.protocol.{Request, Response}
import automorph.spi.Message.Params
import automorph.spi.{Backend, Codec, Message, Transport}
import scala.collection.immutable.ArraySeq
import scala.util.{Random, Try}

trait ClientCore[Node, ExactCodec <: Codec[Node], Effect[_], Context] extends Logging {

  def codec: ExactCodec

  def namedArguments: Boolean

  protected def backend: Backend[Effect]

  protected def transport: Transport[Effect, Context]

  protected def errorToException: (Int, String) => Throwable

  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  /**
   * Perform a method call using specified arguments.
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
  def performCall[R](
    methodName: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    decodeResultNode: Node => R,
    context: Option[Context]
  ): Effect[R] = {
    val argumentNodes = createArgumentNodes(argumentNames, encodedArguments)
    val id = Some(Right[BigDecimal, String](Math.abs(random.nextLong()).toString))
    val formedRequest = Request(id, methodName, argumentNodes).formed
    logger.debug(s"Performing JSON-RPC request", formedRequest.properties)
    backend.flatMap(
      // Serialize request
      serialize(formedRequest),
      (rawRequest: ArraySeq.ofByte) =>
        // Send request
        backend.flatMap(
          transport.call(rawRequest, codec.mediaType, context),
          // Process response
          rawResponse => processResponse[R](rawResponse, formedRequest, decodeResultNode)
        )
    )
  }

  /**
   * Perform a method notification using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param argumentNames argument names
   * @param encodedArguments method argument nodes
   * @param context request context
   * @tparam R result type
   * @return nothing
   */
  protected def performNotify(
    methodName: String,
    argumentNames: Option[Seq[String]],
    encodedArguments: Seq[Node],
    context: Option[Context]
  ): Effect[Unit] = {
    val argumentNodes = createArgumentNodes(argumentNames, encodedArguments)
    val formedRequest = Request(None, methodName, argumentNodes).formed
    backend.flatMap(
      // Serialize request
      serialize(formedRequest),
      // Send request
      (rawRequest: ArraySeq.ofByte) => transport.notify(rawRequest, codec.mediaType, context)
    )
  }

  /**
   * Create method invocation argument nodes.
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
   * Process a method call response.
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
    Try(codec.deserialize(rawResponse)).toEither.fold(
      error => raiseError(ParseErrorException("Invalid response format", error), formedRequest),
      formedResponse => {
        // Validate response
        logger.trace(s"Received JSON-RPC response:\n${codec.format(formedResponse)}")
        Try(Response(formedResponse)).toEither.fold(
          error => raiseError(error, formedRequest),
          validResponse =>
            validResponse.value.fold(
              error => raiseError(errorToException(error.code, error.message), formedRequest),
              result =>
                // Decode result
                Try(decodeResult(result)).toEither.fold(
                  error => raiseError(InvalidResponseException("Invalid result", error), formedRequest),
                  result => {
                    logger.info(s"Performed JSON-RPC request", formedRequest.properties)
                    backend.pure(result)
                  }
                )
            )
        )
      }
    )

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedRequest formed request
   * @return serialized response
   */
  private def serialize(formedRequest: Message[Node]): Effect[ArraySeq.ofByte] = {
    logger.trace(s"Sending JSON-RPC request:\n${codec.format(formedRequest)}")
    Try(codec.serialize(formedRequest)).toEither.fold(
      error => raiseError(ParseErrorException("Invalid request format", error), formedRequest),
      message => backend.pure(message)
    )
  }

  /**
   * Create an error effect from an exception.
   *
   * @param error exception
   * @param requestMessage request message
   * @tparam T effectful value type
   * @return error value
   */
  private def raiseError[T](error: Throwable, requestMessage: Message[Node]): Effect[T] = {
    logger.error(s"Failed to perform JSON-RPC request", error, requestMessage.properties)
    backend.failed(error)
  }
}
