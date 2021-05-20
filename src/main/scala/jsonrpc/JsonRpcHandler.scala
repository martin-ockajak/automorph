package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.EncodingOps.toArraySeq
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{Codec, Effect}
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC handler.
 *
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 */
final case class JsonRpcHandler[Node, Outcome[_]](
  codec: Codec[Node],
  effect: Effect[Outcome],
  private val bindings: Map[String, Node => Node] = Map.empty
):
  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, Outcome] =
    bind("", api)

  inline def bind[T <: AnyRef](methodNamePrefix: String, api: T): JsonRpcHandler[Node, Outcome] =
    val newBindings = ServerMacros.bind(codec, effect, api).map { (name, method) =>
      s"$methodNamePrefix$name" -> method
    }
    JsonRpcHandler(codec, effect, bindings ++ newBindings)

  inline def bind[T, R](methodName: String, function: Tuple => R): JsonRpcHandler[Node, Outcome] =
    ???

  def alias(methodName: String, alias: String): JsonRpcHandler[Node, Outcome] =
    val newBindings = bindings.get(methodName).map(function => alias -> function)
    JsonRpcHandler(codec, effect, bindings ++ newBindings)

  def process(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte] =
    effect.pure(request)

  def process(request: ByteBuffer): Outcome[ByteBuffer] =
    effect.map(process(request.toArraySeq), response => ByteBuffer.wrap(response.unsafeArray))

  def process(request: InputStream): Outcome[InputStream] =
    effect.map(process(request.toArraySeq(bufferSize)), response => ByteArrayInputStream(response.unsafeArray))
