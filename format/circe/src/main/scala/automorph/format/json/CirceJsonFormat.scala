package automorph.format.json

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.EncoderOps
import io.circe.{parser, Decoder, Encoder, Json, JsonObject}
import java.nio.charset.StandardCharsets
import automorph.spi.{Message, MessageError}
import scala.collection.immutable.ArraySeq

/**
 * Circe message format plugin using JSON as message format.
 *
 * @see [[https://circe.github.io/circe Documentation]]
 * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
 * @constructor Creates a Circe format plugin using JSON as message format.
 */
final case class CirceJsonFormat() extends CirceJsonMeta {

  private val charset = StandardCharsets.UTF_8

  implicit private val idEncoder: Encoder[Message.Id] = Encoder.encodeJson.contramap[Message.Id] {
    case Right(id) => Json.fromString(id)
    case Left(id) => Json.fromBigInt(id.toBigInt)
  }

  implicit private val idDecoder: Decoder[Message.Id] = Decoder.decodeJson.map(_.fold(
    invalidId(None.orNull),
    invalidId,
    id => id.toBigDecimal.map(Left.apply).getOrElse(invalidId(id)),
    id => Right(id),
    invalidId,
    invalidId
  ))

  implicit private val paramsEncoder: Encoder[Message.Params[Json]] =
    Encoder.encodeJson.contramap[Message.Params[Json]] {
      case Right(params) => Json.fromJsonObject(JsonObject.fromMap(params))
      case Left(params) => Json.fromValues(params)
    }

  implicit private val paramsDecoder: Decoder[Message.Params[Json]] = Decoder.decodeJson.map(_.fold(
    invalidParams(None.orNull),
    invalidParams,
    invalidParams,
    invalidParams,
    params => Left(params.toList),
    params => Right(params.toMap)
  ))
  implicit private val messageErrorEncoder: Encoder[MessageError[Json]] = deriveEncoder[MessageError[Json]]
  implicit private val messageErrorDecoder: Decoder[MessageError[Json]] = deriveDecoder[MessageError[Json]]
  implicit private val messageEncoder: Encoder[Message[Json]] = deriveEncoder[Message[Json]]
  implicit private val messageDecoder: Decoder[Message[Json]] = deriveDecoder[Message[Json]]

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    new ArraySeq.ofByte(message.asJson.dropNullValues.noSpaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    parser.decode[Message[Json]](new String(data.unsafeArray, charset)).toTry.get

  override def serializeNode(node: Json): ArraySeq.ofByte =
    new ArraySeq.ofByte(node.dropNullValues.noSpaces.getBytes(charset))

  override def deserializeNode(data: ArraySeq.ofByte): Json =
    parser.decode[Json](new String(data.unsafeArray, charset)).toTry.get

  override def format(message: Message[Json]): String = message.asJson.dropNullValues.spaces2

  private def invalidId(value: Any): Message.Id =
    throw new IllegalArgumentException(s"Invalid request identifier: $value")

  private def invalidParams(value: Any): Message.Params[Json] =
    throw new IllegalArgumentException(s"Invalid request parameters: $value")
}

case object CirceJsonFormat {
  /** Message node type. */
  type Node = Json
}
