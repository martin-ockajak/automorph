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
final case class JsonRpcHandler[Node, Outcome[_]] private (
  codec: Codec[Node],
  effect: Effect[Outcome],
)(
  private val methodBindings: Map[String, Node => Node] = Map.empty
):
  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): JsonRpcHandler[Node, Outcome] = bind(api, Seq(_))

  inline def bind[T <: AnyRef](api: T, mapMethod: String => Seq[String]): JsonRpcHandler[Node, Outcome] =
    val bindings = ServerMacros.bind(codec, effect, api).flatMap { (apiMethodName, method) =>
      mapMethod(apiMethodName).map(_ -> method)
    }
    JsonRpcHandler(codec, effect)(methodBindings ++ bindings)

  inline def bind[T, R](method: String, function: Tuple => R): JsonRpcHandler[Node, Outcome] =
    ???

  def process(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte] =
    effect.pure(request)

  def process(request: ByteBuffer): Outcome[ByteBuffer] =
    effect.map(process(request.toArraySeq), response => ByteBuffer.wrap(response.unsafeArray))

  def process(request: InputStream): Outcome[InputStream] =
    effect.map(process(request.toArraySeq(bufferSize)), response => ByteArrayInputStream(response.unsafeArray))

case object JsonRpcHandler:
  def apply[Node, Outcome[_]](codec: Codec[Node], effect: Effect[Outcome]): JsonRpcHandler[Node, Outcome] =
    new JsonRpcHandler(codec, effect)(Map.empty)
