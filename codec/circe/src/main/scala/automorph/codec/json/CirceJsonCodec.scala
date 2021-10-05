package automorph.codec.json

import io.circe.{parser, Decoder, Encoder, Json}
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

/**
 * Circe message codec plugin using JSON format.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @constructor Creates a Circe codec plugin using JSON as message format.
 */
final case class CirceJsonCodec() extends CirceJsonMeta {

  private val charset = StandardCharsets.UTF_8

  override def mediaType: String = "application/json"

  override def serialize(node: Json): ArraySeq.ofByte =
    new ArraySeq.ofByte(node.dropNullValues.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Json =
    parser.decode[Json](new String(data.unsafeArray, charset)).toTry.get

  override def text(node: Json): String = node.dropNullValues.spaces2
}

object CirceJsonCodec {
  implicit lazy val jsonRpcMessageEncoder: Encoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageEncoder
  implicit lazy val jsonRpcMessageDecoder: Decoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageDecoder
  implicit lazy val restRpcMessageEncoder: Encoder[CirceRestRpc.RpcMessage] = CirceRestRpc.messageEncoder
  implicit lazy val restRpcMessageDecoder: Decoder[CirceRestRpc.RpcMessage] = CirceRestRpc.messageDecoder

  /** Message node type. */
  type Node = Json
}
