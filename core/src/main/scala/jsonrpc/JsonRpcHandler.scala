package jsonrpc

import java.beans.IntrospectionException
import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.Protocol.{MethodNotFoundException, ParseErrorException}
import jsonrpc.core.{Empty, Protocol, Request, Response, ResponseError}
import jsonrpc.handler.{HandlerMacros, MethodHandle}
import jsonrpc.log.Logging
import jsonrpc.spi.{Codec, Effect, Message, MessageError}
import jsonrpc.util.EncodingOps.toArraySeq
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome, className}
import jsonrpc.util.CannotEqual
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC request handler layer.
 *
 * The handler can be used by a JSON-RPC server to process incoming JSON-RPC requests, invoke the requested API methods and return JSON-RPC responses.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC request handler using the specified `codec` and `effect` plugins with defined request 'Context' type.
 * @param codec message format codec plugin
 * @param effect effect system plugin
 * @param bufferSize input stream reading buffer size
 * @tparam Node message format node representation type
 * @tparam CodecType message format codec plugin type
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
final case class JsonRpcHandler[Node, CodecType <: Codec[Node], Outcome[_], Context](
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
      HandlerMacros.bind[Node, CodecType, Outcome, Context, T](codec, effect, api).flatMap { (methodName, method) =>
        exposedNames.applyOrElse(
          methodName,
          _ =>
            throw new IntrospectionException(
              s"Bound API does not contain the specified public method: ${api.getClass.getName}.$methodName"
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
    Try(codec.deserialize(rawRequest)).fold(
      error =>
        errorResponse(
          ParseErrorException("Invalid request format", error),
          Message[Node](None, unknownId.asSome, None, None, None, None)
        ),
      formedRequest =>
        // Validate request
        logger.trace(s"Received JSON-RPC message:\n${codec.format(formedRequest)}")
        Try(Request(formedRequest)).fold(
          error => errorResponse(error, formedRequest),
          validRequest => invokeMethod(formedRequest, validRequest, context)
        )
    )

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
      // Extract arguments
      val contextEmpty = context.isInstanceOf[Empty[?]] || context.isInstanceOf[None.type] || context.isInstanceOf[Unit]
      val arguments = extractArguments(validRequest, contextEmpty, methodHandle)

      // Invoke method
      Try(effect.either(methodHandle.function(arguments, context))).fold(
        error => errorResponse(error, formedRequest),
        outcome =>
          effect.flatMap(
            outcome,
            // Process result
            _.fold(
              error => errorResponse(error, formedRequest),
              result =>
                validRequest.id.map { id =>
                  // Serialize response
                  val validResponse = Response(id, result.asRight)
                  logger.info(s"Processed JSON-RPC request", formedRequest.properties)
                  serialize(validResponse.formed)
                }.getOrElse(effect.pure(None))
            )
          )
      )
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
   * @param contextEmpty request context is empty
   * @param methodHandle bound method handle
   * @return bound method arguments
   */
  private def extractArguments(
    validRequest: Request[Node],
    contextEmpty: Boolean,
    methodHandle: MethodHandle[Node, Outcome, Context]
  ): Seq[Node] =
    val parameters = methodHandle.paramNames.dropRight(if contextEmpty then 0 else 1)
    validRequest.params.fold(
      arguments =>
        // Arguments by position
        if arguments.size < parameters.size then
          throw IllegalArgumentException(s"Missing arguments: ${parameters.drop(arguments.size)}")
        else if arguments.size > parameters.size then
          throw IllegalArgumentException(s"Redundant arguments: ${parameters.size - arguments.size}")
        arguments
      ,
      namedArguments =>
        // Arguments by name
        val arguments = parameters.flatMap(namedArguments.get)
        if arguments.size < parameters.size then
          throw IllegalArgumentException(s"Missing arguments: ${parameters.filterNot(namedArguments.contains)}")
        else if arguments.size > parameters.size then
          throw IllegalArgumentException(s"Redundant arguments: ${namedArguments.keys.filterNot(parameters.contains)}")
        arguments
    )

  /**
   * Create an error response for a request.
   *
   * @param error exception
   * @param formedRequest formed request
   * @return error response if applicable
   */
  private def errorResponse(error: Throwable, formedRequest: Message[Node]): Outcome[Option[ArraySeq.ofByte]] =
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    formedRequest.id.map { id =>
      // Assemble error details
      val code = Protocol.exceptionError(error.getClass).code
      val errorDetails = Protocol.errorDetails(error)
      val message = errorDetails.headOption.getOrElse("Unknown error")
      val data = encodeStrings(errorDetails.drop(1)).asSome

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
    Try(codec.serialize(formedMessage)).fold(
      error => effect.failed(ParseErrorException("Invalid message format", error)),
      message => effect.pure(message.asSome)
    )

  override def toString: String =
    val codecName = codec.className
    val effectName = effect.className
    val boundMethods = methodBindings.size
    s"$JsonRpcHandler(Codec: $codecName, Effect: $effectName, Bound methods: $boundMethods)"

case object JsonRpcHandler:

  type NoContext = Empty[JsonRpcHandler[?, ?, ?, ?]]
  given NoContext = Empty[JsonRpcHandler[?, ?, ?, ?]]()

  /**
   * Create a new JSON-RPC request handler using the specified `codec` and `effect` plugins without request 'Context' type.
   *
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message format codec plugin
   * @param effect computation effect system plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam Outcome computation outcome effect type
   * @return JSON-RPC request handler
   */
  inline def basic[Node, CodecType <: Codec[Node], Outcome[_]](
    codec: CodecType,
    effect: Effect[Outcome],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Outcome, NoContext] =
    new JsonRpcHandler(codec, effect, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))

  /**
   * Create a new JSON-RPC request handler using the specified `codec` and `effect` plugins with defined request 'Context' type.
   *
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message format codec plugin
   * @param effect computation effect system plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam Outcome computation outcome effect type
   * @return JSON-RPC request handler
   */
  inline def apply[Node, CodecType <: Codec[Node], Outcome[_], Context](
    codec: CodecType,
    effect: Effect[Outcome],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Outcome, Context] =
    new JsonRpcHandler(codec, effect, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))
