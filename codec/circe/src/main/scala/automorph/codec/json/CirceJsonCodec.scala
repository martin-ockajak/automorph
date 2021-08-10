package automorph.codec.json

import io.circe.{Decoder, Encoder, Json, parser}
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

/**
 * Circe message codec plugin using JSON as message codec.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @constructor Creates a Circe codec plugin using JSON as message codec.
 */
final case class CirceJsonCodec() extends CirceJsonMeta {

  private val charset = StandardCharsets.UTF_8
  implicit private lazy val jsonRpcMessageEncoder: Encoder[CirceJsonRpc.Data] = CirceJsonRpc.messageEncoder
  implicit private lazy val jsonRpcMessageDecoder: Decoder[CirceJsonRpc.Data] = CirceJsonRpc.messageDecoder
  implicit private lazy val restRpcMessageEncoder: Encoder[CirceRestRpc.Data] = CirceRestRpc.messageEncoder
  implicit private lazy val restRpcMessageDecoder: Decoder[CirceRestRpc.Data] = CirceRestRpc.messageDecoder

  override def mediaType: String = "application/json"

  override def serialize(node: Json): ArraySeq.ofByte =
    new ArraySeq.ofByte(node.dropNullValues.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Json =
    parser.decode[Json](new String(data.unsafeArray, charset)).toTry.get

  override def text(node: Json): String = node.dropNullValues.spaces2
}

case object CirceJsonCodec {
  /** Message node type. */
  type Node = Json
}
