package automorph.codec.json

import automorph.protocol.jsonrpc.Message
import io.circe.{Decoder, Encoder, Json, parser}
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

/**
 * Circe JSON message codec plugin.
 *
 * @see [[https://www.json.org Message format]]
 * @see [[https://circe.github.io/circe Library documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @constructor Creates a Circe codec plugin using JSON as message format.
 */
final case class CirceJsonCodec() extends CirceJsonMeta {

  override val mediaType: String = "application/json"

  override def serialize(node: Json): ArraySeq.ofByte =
    new ArraySeq.ofByte(node.dropNullValues.noSpaces.getBytes(CirceJsonCodec.charset))

  override def deserialize(data: ArraySeq.ofByte): Json =
    parser.decode[Json](new String(data.unsafeArray, CirceJsonCodec.charset)).toTry.get

  override def text(node: Json): String = node.dropNullValues.spaces2
}

object CirceJsonCodec {

  /** Message node type. */
  type Node = Json

  implicit lazy val jsonRpcMessageEncoder: Encoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageEncoder
  implicit lazy val jsonRpcMessageDecoder: Decoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageDecoder
  implicit lazy val restRpcMessageEncoder: Encoder[CirceRestRpc.RpcMessage] = CirceRestRpc.messageEncoder
  implicit lazy val restRpcMessageDecoder: Decoder[CirceRestRpc.RpcMessage] = CirceRestRpc.messageDecoder
  private val charset = StandardCharsets.UTF_8
}
