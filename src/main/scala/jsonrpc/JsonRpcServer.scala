package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.EncodingOps.asArraySeq
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{Codec, Effect}
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC server.
 *
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 */
final case class JsonRpcServer[Node, Outcome[_]](
  codec: Codec[Node],
  effect: Effect[Outcome],
  private val bindings: Map[String, Node => Node] = Map.empty
):
  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): JsonRpcServer[Node, Outcome] =
    val newBindings = ServerMacros.bind(codec, effect, api)
    JsonRpcServer(codec, effect, bindings ++ newBindings)

  def process(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte] =
    effect.pure(request)

  def process(request: ByteBuffer): Outcome[ByteBuffer] =
    effect.map(process(request.asArraySeq), response => {
      ByteBuffer.wrap(response.unsafeArray).nn
    })

  def process(request: InputStream): Outcome[InputStream] =
    effect.map(process(request.asArraySeq(bufferSize)), response => {
      ByteArrayInputStream(response.unsafeArray)
    })
