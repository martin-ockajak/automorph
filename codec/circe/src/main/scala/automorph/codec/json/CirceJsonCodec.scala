package automorph.codec.json

import automorph.codec.json.meta.CirceJsonMeta
import automorph.schema.{OpenApi, OpenRpc}
import automorph.util.Extensions.{InputStreamOps, StringOps}
import io.circe.{parser, Decoder, Encoder, Json}
import java.io.InputStream

/**
 * Circe JSON message codec plugin.
 *
 * @see
 *   [[https://www.json.org Message format]]
 * @see
 *   [[https://circe.github.io/circe Library documentation]]
 * @see
 *   [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @constructor
 *   Creates a Circe codec plugin using JSON as message format.
 */
final case class CirceJsonCodec() extends CirceJsonMeta {

  override val mediaType: String = "application/json"

  override def serialize(node: Json): InputStream =
    node.dropNullValues.noSpaces.toInputStream

  override def deserialize(data: InputStream): Json =
    parser.decode[Json](data.asString).toTry.get

  override def text(node: Json): String =
    node.dropNullValues.spaces2
}

case object CirceJsonCodec {

  /** Message node type. */
  type Node = Json

  implicit lazy val jsonRpcMessageEncoder: Encoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageEncoder
  implicit lazy val jsonRpcMessageDecoder: Decoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageDecoder
  implicit lazy val restRpcMessageEncoder: Encoder[CirceWebRpc.RpcMessage] = CirceWebRpc.messageEncoder
  implicit lazy val restRpcMessageDecoder: Decoder[CirceWebRpc.RpcMessage] = CirceWebRpc.messageDecoder
  implicit lazy val openRpcEncoder: Encoder[OpenRpc] = CirceOpenRpc.openRpcEncoder
  implicit lazy val openRpcDecoder: Decoder[OpenRpc] = CirceOpenRpc.openRpcDecoder
  implicit lazy val openApiEncoder: Encoder[OpenApi] = CirceOpenApi.openApiEncoder
  implicit lazy val openApiDecoder: Decoder[OpenApi] = CirceOpenApi.openApiDecoder
}
