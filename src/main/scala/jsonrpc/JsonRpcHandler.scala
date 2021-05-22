package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.core.Errors
import jsonrpc.core.Protocol.Id
import jsonrpc.core.Protocol.ParseErrorException
import jsonrpc.core.Protocol.MethodNotFoundException
import jsonrpc.core.Protocol
import jsonrpc.core.Response
import jsonrpc.core.Request
import jsonrpc.spi.Message
import jsonrpc.spi.CallError
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome, className}
import jsonrpc.server.{HandlerMacros, MethodHandle}
import jsonrpc.spi.{Codec, Effect}
import jsonrpc.util.CannotEqual
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
 * @tparam Outcome computation outcome effect type
 * @tparam Context request context type
 */
final case class JsonRpcHandler[Node, CodecType <: Codec[Node], Outcome[_], Context] private (
  codec: CodecType,
  effect: Effect[Outcome],
  bufferSize: Int,
  private val methodBindings: Map[String, MethodHandle[Node, Outcome, Context]]
) extends CannotEqual:

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
  inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, CodecType, Outcome, Context] = bind(api, Seq(_))

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
    bind(api, Function.unlift(exposedNames.andThen(asSome)))

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
      HandlerMacros.bind[T, Node, Outcome, codec.type, Context](codec, effect, api).flatMap { (apiMethodName, method) =>
        exposedNames.applyOrElse(
          apiMethodName,
          throw new IllegalArgumentException(
            s"Bound API does not contain the specified public method: ${api.getClass.getName}.$apiMethodName"
          )
        ).map(_ -> method)
      }
    copy(methodBindings = methodBindings ++ bindings)

  /**
   * Create a new JSON-RPC request handler by adding a binding for the specified function.
   *
   * The bound function definition may contain an `using` clause defining a single ''context parameter'' of `Context` type.
   * The bound function may return a context fuction consuming a single ''context parameter'' of `Context` type.
   * Server-supplied ''request context'' is then passed to the returned context function as the ''context parameter'' argument.
   *
   * @param method JSON-RPC method name
   * @param api API instance
   * @param exposedNames transform API type method name to its exposed JSON-RPC method names
   * @tparam T API type
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T, R](method: String, function: Tuple => R): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    ???

  /**
   * Invoke a ''bound method'' specified in a JSON-RPC ''request'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @return optional JSON-RPC response message and context
   */
  def process(request: ArraySeq.ofByte): Outcome[Option[ArraySeq.ofByte]] = process(request, None)

  /**
   * Invokes a ''bound method'' specified in a JSON-RPC ''request'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @return optional JSON-RPC response message
   */
  def process(request: ByteBuffer): Outcome[Option[ByteBuffer]] = process(request, None)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @return optional JSON-RPC response message
   */
  def process(request: InputStream): Outcome[Option[InputStream]] = process(request, None)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' plus an additional ''request context'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @param context request context
   * @return optional JSON-RPC response message
   */
  def process(request: ArraySeq.ofByte, context: Option[Context]): Outcome[Option[ArraySeq.ofByte]] =
    handle(request, context)

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' plus an additional ''request context'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @param context request context
   * @return optional JSON-RPC response message
   */
  def process(request: ByteBuffer, context: Option[Context]): Outcome[Option[ByteBuffer]] =
    effect.map(process(request.toArraySeq), _.map(response => ByteBuffer.wrap(response.unsafeArray)))

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' plus an additional ''request context'' and return a JSON-RPC ''response''.
   *
   * @param request JSON-RPC request message
   * @param context request context
   * @return optional JSON-RPC response message
   */
  def process(request: InputStream, context: Option[Context]): Outcome[Option[InputStream]] =
    effect.map(process(request.toArraySeq(bufferSize)), _.map(response => ByteArrayInputStream(response.unsafeArray)))

  private def handle(message: ArraySeq.ofByte, context: Option[Context]): Outcome[Option[ArraySeq.ofByte]] =
    Try(codec.deserialize(message)) match
      case Success(validMessage) => Try(Request(validMessage)) match
          case Success(request) => invoke(request, context)
          case Failure(error)   => effect.pure(errorResponse(error, validMessage.id))
      case Failure(error) =>
        effect.pure(errorResponse(ParseErrorException("Invalid request format", error), unknownId.asSome))

  private def invoke(request: Request[Node], context: Option[Context]): Outcome[Option[ArraySeq.ofByte]] =
    methodBindings.get(request.method).map { methodHandle =>
      val arguments = extractArguments(request, methodHandle)
      Try(effect.either(methodHandle.function(arguments, None))) match
        case Success(outcome) => effect.map(outcome, _ match
          case Right(result) => request.id.flatMap { id =>
            val response = Response(id, result.asRight)
            serialize(response)
          }
          case Left(error) => errorResponse(error, request.id)
        )
        case Failure(error) => effect.pure(errorResponse(error, request.id))
    }.getOrElse {
      effect.pure(errorResponse(MethodNotFoundException(s"Method not found: ${request.method}", None.orNull), request.id))
    }

  private def extractArguments(request: Request[Node], methodHandle: MethodHandle[Node, Outcome, Context]): Seq[Node] =
    val params = methodHandle.paramNames
    request.params match
      case Left(arguments) =>
        if arguments.size < params.size then
          throw IllegalArgumentException(s"Missing arguments: ${params.drop(arguments.size)}")
        else if arguments.size > params.size then
          throw IllegalArgumentException(s"Redundant arguments: ${params.size - arguments.size}")
        arguments
      case Right(namedArguments) =>
        val arguments = params.flatMap(namedArguments.get)
        if arguments.size < params.size then
          throw IllegalArgumentException(s"Missing arguments: ${params.filterNot(namedArguments.contains)}")
        else if arguments.size > params.size then
          throw IllegalArgumentException(s"Redundant arguments: ${namedArguments.keys.filterNot(params.contains)}")
        arguments

  /**
   * Create JSON-RPC error response.
   *
   * @param error exception
   * @param requestId request identifier
   * @return error response if applicable
   */
  private def errorResponse(error: Throwable, requestId: Option[Id]): Option[ArraySeq.ofByte] =
    requestId.flatMap { id =>
      val code = Protocol.exceptionError(error.getClass).code
      val (message, data) = Errors.descriptions(error) match
        case Seq(message, details*) => message -> codec.encode(details).asSome
        case Seq()                  => "Unknown error" -> Option.empty[Node]
      val response = Response[Node](id, CallError[Node](code.asSome, message.asSome, data).asLeft)
      serialize(response)
    }

  /**
   * Serialize JSON-RPC response.
   *
   * @param response response
   * @return serialized response
   */
  private def serialize(response: Response[Node]): Option[ArraySeq.ofByte] =
    Try(codec.serialize(response.message)) match
      case Success(message) => message.asSome
      case Failure(error) =>
        // FIXME - log and escalate the error here
        None

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
