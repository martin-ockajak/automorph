package jsonrpc

import java.beans.IntrospectionException
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.core.Protocol.{Id, MethodNotFoundException, ParseErrorException}
import jsonrpc.core.{Errors, Protocol, Request, Response, ResponseError}
import jsonrpc.handler.{HandlerMacros, MethodHandle}
import jsonrpc.log.Logging
import jsonrpc.spi.{Codec, Effect, Message, MessageError}
import jsonrpc.util.CannotEqual
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome, className}
import scala.collection.immutable.ArraySeq
import scala.compiletime.erasedValue
import scala.util.{Failure, Success, Try}

/**
 * JSON-RPC request handler layer.
 *
 * The handler can be used by a JSON-RPC server to process incoming JSON-RPC requests, invoke the requested API methods and return JSON-RPC responses.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC handler using the specified `codec` and `effect` implementations.
 * @param codec data format codec plugin
 * @param effect effect system plugin
 * @param bufferSize input stream reading buffer size
 * @tparam Node data format node representation type
 * @tparam CodecType data format codec plugin type
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
final case class JsonRpcHandler[Node, CodecType <: Codec[Node], Outcome[_], Context] (
  codec: CodecType,
  effect: Effect[Outcome],
  bufferSize: Int,
  private val methodBindings: Map[String, MethodHandle[Node, Outcome, Context]],
  private val encodeStrings: Seq[String] => Node
) extends CannotEqual with Logging:

  private val unknownId = "[unknown]".asRight

  /**
   * Create a new JSON-RPC request handler while generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfied all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context fuction accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * API methods are exposed using their actual names.
   *
   * @param api API instance
   * @tparam T API type (only member methods of this types are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, CodecType, Outcome, Context] = bind(api, name => Seq(name))

  /**
   * Create a new JSON-RPC request handler while generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfied all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context fuction accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * API methods are exposed using names resulting from a transformation of their actual names via the `exposedNames` function.
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its actual name (empty result causes the method not to be exposed)
   * @tparam T API type (only member methods of this types are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](
    api: T,
    exposedNames: String => Seq[String]
  ): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    bind(api, Function.unlift(name => Some(exposedNames(name))))

  /**
   * Create a new JSON-RPC request handler while generating method bindings for all valid public methods of the specified API.
   *
   * A method is considered valid if it satisfied all of these conditions:
   * - can be called at runtime
   * - has no type parameters
   * - returns the specified effect type
   * - (if request context type is not Unit) accepts the specified request context type as its last parameter
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context fuction accepting one
   * the server-supplied ''request context'' is passed to the bound method or the returned context function as its last argument.
   *
   * API methods are exposed using names resulting from a transformation of their actual names via the `exposedNames` function.
   *
   * @param api API instance
   * @param exposedNames create exposed method names from its actual name (empty result causes the method not to be exposed)
   * @tparam T API type (only member methods of this types are exposed)
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
            throw new IntrospectionException(
              s"Bound API does not contain the specified public method: ${api.getClass.getName}.$apiMethodName"
            )
        ).map(_ -> method)
      }
    copy(methodBindings = methodBindings ++ bindings)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: ArraySeq.ofByte)(using context: Context): Outcome[Option[ArraySeq.ofByte]] =
    handleRequest(request, context)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: ByteBuffer)(using context: Context): Outcome[Option[ByteBuffer]] =
    effect.map(processRequest(request.toArraySeq)(using context), _.map(response => ByteBuffer.wrap(response.unsafeArray)))

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: InputStream)(using context: Context): Outcome[Option[InputStream]] =
    effect.map(
      processRequest(request.toArraySeq(bufferSize))(using context),
      _.map(response => ByteArrayInputStream(response.unsafeArray))
    )

  /**
   * Handle a JSON-RPC request.
   *
   * @param rawRequest raw request
   * @param context request context
   * @return response message
   */
  private def handleRequest(
    rawRequest: ArraySeq.ofByte,
    context: Context
  ): Outcome[Option[ArraySeq.ofByte]] =
    // Deserialize request
    Try(codec.deserialize(rawRequest)) match
      case Success(formedRequest) =>
        // Validate request
        logger.trace(s"Received JSON-RPC message:\n${codec.format(formedRequest)}")
        Try(Request(formedRequest)) match
          case Success(validRequest) => invokeMethod(formedRequest, validRequest, context)
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
  private def invokeMethod(
    formedRequest: Message[Node],
    validRequest: Request[Node],
    context: Context
  ): Outcome[Option[ArraySeq.ofByte]] =
    logger.debug(s"Processing JSON-RPC request", formedRequest.properties)
    methodBindings.get(validRequest.method).map { methodHandle =>
      // Invoke method
      val contextSupplied = context.isInstanceOf[None.type] || context.isInstanceOf[Unit]
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
  private def errorResponse(error: Throwable, formedRequest: Message[Node]): Outcome[Option[ArraySeq.ofByte]] =
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    formedRequest.id.map { id =>
      // Assemble error details
      val code = Protocol.exceptionError(error.getClass).code
      val descriptions = Errors.descriptions(error)
      val message = descriptions.headOption.getOrElse("Unknown error")
      val data = encodeStrings(descriptions.drop(1)).asSome

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
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @param codec hierarchical data format codec plugin
   * @param effect computation effect system plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node data format node representation type
   * @tparam Outcome computation outcome effect type
   */
  inline def apply[Node, CodecType <: Codec[Node], Outcome[_]](
    codec: CodecType,
    effect: Effect[Outcome],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Outcome, None.type] =
    new JsonRpcHandler(codec, effect, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))

  /**
   * Create a JSON-RPC request handler with specified request context type.
   *
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @param codec hierarchical data format codec plugin
   * @param effect computation effect system plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node data format node representation type
   * @tparam Outcome computation outcome effect type
   * @tparam Context JSON-RPC call context type
   */
  inline def withContext[Node, CodecType <: Codec[Node], Outcome[_], Context](
    codec: CodecType,
    effect: Effect[Outcome],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    new JsonRpcHandler(codec, effect, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))
