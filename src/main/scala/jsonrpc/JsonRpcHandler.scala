package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.core.Protocol.{Id, MethodNotFoundException, ParseErrorException}
import jsonrpc.core.{Errors, Protocol, Request, Response, ResponseError}
import jsonrpc.log.Logging
import jsonrpc.handler.{HandlerMacros, MethodHandle}
import jsonrpc.spi.{Codec, Effect, Message, MessageError}
import jsonrpc.util.CannotEqual
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome, className}
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

/**
 * JSON-RPC request handler.
 *
 * The handler can be used by to process incoming JSON-RPC requests and create JSON-RPC responses.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC handler using the specified `codec` and `effect` implementations.
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @param bufferSize input stream reading buffer size
 * @tparam Node data format node representation type
 * @tparam CodecType data format codec plugin type
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
final case class JsonRpcHandler[Node, CodecType <: Codec[Node], Outcome[_], Context] private (
  codec: CodecType,
  effect: Effect[Outcome],
  bufferSize: Int,
  private val methodBindings: Map[String, MethodHandle[Node, Outcome, Context]]
) extends CannotEqual with Logging:

  private val unknownId = "[unknown]".asRight

  /**
   * Create a new JSON-RPC request handler by adding method bindings for member methods of the specified API.
   *
   * Used by JSON-RPC server implementations to process JSON-RPC requests into JSON-RPC responses.
   *
   * Generates JSON-RPC bindings for all valid public methods of the API type.
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * - have type parameters
   * - cannot be called at runtime
   *
   * A bound method definition may include a last ''context parameter'' of `Context` type or return a context fuction consuming one.
   * Server-supplied ''request context'' is then passed to the bound method or the returned context function as the 'context parameter' argument.
   *
   * API methods are exposed using their actual names.
   *
   * @param api API instance
   * @tparam T API type (only its member methds are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, CodecType, Outcome, Context] = bind(api, name => Seq(name))

  /**
   * Create a new JSON-RPC request handler by adding method bindings for member methods of the specified API.
   *
   * Generates JSON-RPC bindings for all valid public methods of the API type.
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * - have type parameters
   * - cannot be called at runtime
   *
   * A bound method definition may include a last ''context parameter'' of `Context` type or return a context fuction consuming one.
   * Server-supplied ''request context'' is then passed to the bound method or the returned context function as the ''context parameter'' argument.
   *
   * API methods are exposed using names their local names transformed by the .
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its name (empty result causes the method not to be exposed)
   * @tparam T API type (only its member methds are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](
    api: T,
    exposedNames: String => Seq[String]
  ): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    bind(api, Function.unlift(name => Some(exposedNames(name))))

  /**
   * Create a new JSON-RPC request handler by adding method bindings for member methods of the specified API.
   *
   * Generates JSON-RPC bindings for all valid public methods of the API type.
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * - have type parameters
   * - cannot be called at runtime
   *
   * A bound method definition may include a last ''context parameter'' of `Context` type or return a context fuction consuming one.
   * Server-supplied ''request context'' is then passed to the bound method or the returned context function as the ''context parameter'' argument.
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its name (empty result causes the method not to be exposed)
   * @tparam T API type (only its member methds are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](
    api: T,
    exposedNames: PartialFunction[String, Seq[String]]
  ): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    val bindings =
      HandlerMacros.bind[Node, CodecType, Outcome, Context, T](codec, effect, api).flatMap { (apiMethodName, method) =>
        exposedNames.applyOrElse(
          apiMethodName,
          _ =>
            throw new IllegalArgumentException(
              s"Bound API does not contain the specified public method: ${api.getClass.getName}.$apiMethodName"
            )
        ).map(_ -> method)
      }
    copy(methodBindings = methodBindings ++ bindings)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @param context request context
   * @return optional JSON-RPC response message
   */
  inline def processRequest(request: ArraySeq.ofByte, context: Context): Outcome[Option[ArraySeq.ofByte]] =
    handleRequest(request, context)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @param context request context
   * @return optional JSON-RPC response message
   */
  inline def processRequest(request: ByteBuffer, context: Context): Outcome[Option[ByteBuffer]] =
    effect.map(processRequest(request.toArraySeq, context), _.map(response => ByteBuffer.wrap(response.unsafeArray)))

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @param context request context
   * @return optional JSON-RPC response message
   */
  inline def processRequest(request: InputStream, context: Context): Outcome[Option[InputStream]] =
    effect.map(
      processRequest(request.toArraySeq(bufferSize), context),
      _.map(response => ByteArrayInputStream(response.unsafeArray))
    )

  /**
   * Handle a JSON-RPC request.
   *
   * @param rawRequest raw request
   * @param context request context
   * @return response message
   */
  private inline def handleRequest(
    rawRequest: ArraySeq.ofByte,
    context: Context
  ): Outcome[Option[ArraySeq.ofByte]] =
    // Deserialize request
    Try(codec.deserialize(rawRequest)) match
      case Success(formedRequest) =>
        // Validate request
        logger.trace(s"Received JSON-RPC message:\n${codec.format(formedRequest)}")
        Try(Request(formedRequest)) match
          case Success(validRequest) => invoke(formedRequest, validRequest, context)
          case Failure(error)        => errorResponse(error, formedRequest)
      case Failure(error) =>
        val virtualMessage = Message[Node](None, unknownId.asSome, None, None, None, None)
        errorResponse(ParseErrorException("Invalid request format", error), virtualMessage)

  /**
   * Invoke bound method specified in a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param formedRequest formed request
   * @param validRequest valid request
   * @param context request context
   * @return bound method invocation outcome
   */
  private inline def invoke(
    formedRequest: Message[Node],
    validRequest: Request[Node],
    context: Context
  ): Outcome[Option[ArraySeq.ofByte]] =
    logger.debug(s"Processing JSON-RPC request", formedRequest.properties)
    methodBindings.get(validRequest.method).map { methodHandle =>
      // Invoke method
      val contextSupplied = (context.isInstanceOf[AnyVal] || context.isInstanceOf[AnyRef]) &&
        !context.isInstanceOf[Unit] &&
        !context.isInstanceOf[Nothing]
      val arguments = extractArguments(validRequest, contextSupplied, methodHandle)
      Try(effect.either(methodHandle.function(arguments, context))) match
        case Success(outcome) => effect.flatMap(
            outcome,
            _ match
              case Right(result) => validRequest.id.map { id =>
                  // Serialize response
                  val validResponse = Response(id, result.asRight)
                  logger.info(s"Processed JSON-RPC request", formedRequest.properties)
                  serialize(validResponse.formed)
                }.getOrElse(effect.pure(None))
              case Left(error) => errorResponse(error, formedRequest)
          )
        case Failure(error) => errorResponse(error, formedRequest)
    }.getOrElse {
      errorResponse(
        MethodNotFoundException(s"Method not found: ${validRequest.method}", None.orNull),
        formedRequest
      )
    }

  /**
   * Validata and extract specified bound method arguments from a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param validRequest valid request
   * @param contextSupplied request context supplied
   * @param methodHandle bound method handle
   * @return bound method arguments
   */
  private def extractArguments(
    validRequest: Request[Node],
    contextSupplied: Boolean,
    methodHandle: MethodHandle[Node, Outcome, Context]
  ): Seq[Node] =
    val parameters = methodHandle.paramNames.dropRight(if contextSupplied then 1 else 0)
    validRequest.params match
      case Left(arguments) =>
        // Arguments by position
        if arguments.size < parameters.size then
          throw IllegalArgumentException(s"Missing arguments: ${parameters.drop(arguments.size)}")
        else if arguments.size > parameters.size then
          throw IllegalArgumentException(s"Redundant arguments: ${parameters.size - arguments.size}")
        arguments
      case Right(namedArguments) =>
        // Arguments by name
        val arguments = parameters.flatMap(namedArguments.get)
        if arguments.size < parameters.size then
          throw IllegalArgumentException(s"Missing arguments: ${parameters.filterNot(namedArguments.contains)}")
        else if arguments.size > parameters.size then
          throw IllegalArgumentException(s"Redundant arguments: ${namedArguments.keys.filterNot(parameters.contains)}")
        arguments

  /**
   * Create an error response for a request.
   *
   * @param error exception
   * @param formedRequest formed request
   * @param requestId request identifier
   * @return error response if applicable
   */
  private inline def errorResponse(error: Throwable, formedRequest: Message[Node]): Outcome[Option[ArraySeq.ofByte]] =
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    formedRequest.id.map { id =>
      // Assemble error details
      val code = Protocol.exceptionError(error.getClass).code
      val descriptions = Errors.descriptions(error)
      val message = descriptions.headOption.getOrElse("Unknown error")
      val data = codec.encode(descriptions.drop(1)).asSome

      // Serialize response
      val validResponse = Response[Node](id, ResponseError(code, message, data).asLeft)
      serialize(validResponse.formed)
    }.getOrElse(effect.pure(None))

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedMessage JSON-RPC message
   * @return serialized response
   */
  private def serialize(formedMessage: Message[Node]): Outcome[Option[ArraySeq.ofByte]] =
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(formedMessage)}")
    Try(codec.serialize(formedMessage)) match
      case Success(message) => effect.pure(message.asSome)
      case Failure(error)   => effect.failed(ParseErrorException("Invalid message format", error))

  override def toString =
    val codecName = codec.className
    val effectName = effect.className
    val boundMethods = methodBindings.size
    s"$JsonRpcHandler(Codec: $codecName, Effect: $effectName, Bound methods: $boundMethods)"

case object JsonRpcHandler:

  /**
   * Create a JSON-RPC request handler.
   *
   * The handler can be used to process incoming JSON-RPC requests and create JSON-RPC responses.
   *
   * @param codec hierarchical data format codec plugin
   * @param effect computation effect system plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node data format node representation type
   * @tparam Outcome computation outcome effect type
   * @tparam Context JSON-RPC call context type
   */
  def apply[Node, CodecType <: Codec[Node], Outcome[_], Context](
    codec: CodecType,
    effect: Effect[Outcome],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    new JsonRpcHandler(codec, effect, bufferSize, Map.empty)
