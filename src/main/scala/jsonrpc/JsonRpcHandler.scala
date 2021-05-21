package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.core.ValueOps.asSome
import jsonrpc.server.HandlerMacros
import jsonrpc.server.FunctionHandle
import jsonrpc.spi.{Codec, Effect}
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC request handler.
 *
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 */
final case class JsonRpcHandler[Node, Outcome[_]] private (
  codec: Codec[Node],
  effect: Effect[Outcome]
)(
  private val methodBindings: Map[String, FunctionHandle[Node, Outcome]] = Map.empty
):

  private val bufferSize = 4096

  /**
   * Create a new JSON-RPC request handler by adding method bindings for member methods of the specified API.
   *
   * Generates JSON-RPC bindings for all valid public methods of the API type.
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * * have type parameters
   * * cannot be called at runtime
   *
   * API methods are exposed using their actual names.
   *
   * @param api API instance
   * @tparam T API type (only its member methds are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, Outcome] = bind(api, Seq(_))

  /**
   * Create a new JSON-RPC request handler by adding method bindings for member methods of the specified API.
   *
   * Generates JSON-RPC bindings for all valid public methods of the API type.
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * * have type parameters
   * * cannot be called at runtime
   *
   * API methods are exposed using names their local names transformed by the .
   *
   * @param api API instance
   * @param exposedNames creates exposed method names from its name (empty result causes the method not to be exposed)
   * @tparam T API type (only its member methds are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](api: T, exposedNames: String => Seq[String]): JsonRpcHandler[Node, Outcome] =
    bind(api, Function.unlift(exposedNames.andThen(asSome)))

  /**
   * Create a new JSON-RPC request handler by adding method bindings for member methods of the specified API.
   *
   * Generates JSON-RPC bindings for all valid public methods of the API type.
   * Throws an exception if an invalid public method is found.
   * Methods are considered invalid if they satisfy one of these conditions:
   * * have type parameters
   * * cannot be called at runtime
   *
   * @param api API instance
   * @param exposedNames creates exposed method names from its name (empty result causes the method not to be exposed)
   * @tparam T API type (only its member methds are exposed)
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T <: AnyRef](api: T, exposedNames: PartialFunction[String, Seq[String]]): JsonRpcHandler[Node, Outcome] =
    val bindings = HandlerMacros.bind(codec, effect, api).flatMap { (apiMethodName, method) =>
      exposedNames.applyOrElse(
        apiMethodName,
        throw new IllegalArgumentException(s"Bound API does not contain the specified public method: ${api.getClass.getName}.$apiMethodName")
      ).map(_ -> method)
    }
    JsonRpcHandler(codec, effect)(methodBindings ++ bindings)

  /**
   * Creates a new JSON-RPC request handler by adding a binding for the specified function.
   *
   * @param method JSON-RPC method name
   * @param api API instance
   * @param exposedNames transform API type method name to its exposed JSON-RPC method names
   * @tparam T API type
   * @return JSON-RPC server including the additional API bindings
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[T, R](method: String, function: Tuple => R): JsonRpcHandler[Node, Outcome] =
    ???

  /**
   * Invokes a bound method specified in a JSON-RPC request and creates a JSON-RPC response.
   *
   * @param request JSON-RPC request message
   * @return JSON-RPC response message
   */
  def process(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte] =
    effect.pure(request)

  /**
   * Invokes a bound method specified in a JSON-RPC request and creates a JSON-RPC response.
   *
   * @param request JSON-RPC request message
   * @return JSON-RPC response message
   */
  def process(request: ByteBuffer): Outcome[ByteBuffer] =
    effect.map(process(request.toArraySeq), response => ByteBuffer.wrap(response.unsafeArray))

  /**
   * Invokes a bound method specified in a JSON-RPC request and creates a JSON-RPC response.
   *
   * @param request JSON-RPC request message
   * @return JSON-RPC response message
   */
  def process(request: InputStream): Outcome[InputStream] =
    effect.map(process(request.toArraySeq(bufferSize)), response => ByteArrayInputStream(response.unsafeArray))

case object JsonRpcHandler:

  def apply[Node, Outcome[_]](codec: Codec[Node], effect: Effect[Outcome]): JsonRpcHandler[Node, Outcome] =
    new JsonRpcHandler(codec, effect)(Map.empty)
