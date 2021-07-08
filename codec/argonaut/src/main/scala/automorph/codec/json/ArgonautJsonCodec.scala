package automorph.codec.json

import argonaut.Argonaut.{jNull, StringToParseWrap, ToJsonIdentity}
import argonaut.{Argonaut, CodecJson, DecodeResult, Json}
import java.nio.charset.StandardCharsets
import automorph.spi.{Message, MessageError}
import scala.collection.immutable.ArraySeq

/**
 * Argonaut codec plugin using JSON as message format.
 *
 * @see [[http://argonaut.io/doc/]]
 * @see [[http://argonaut.io/scaladocs/#argonaut.Json Node type]]
 */
final case class ArgonautJsonCodec() extends ArgonautJsonCodecMeta {

  private val charset = StandardCharsets.UTF_8

  implicit private lazy val messageErrorCodecJson: CodecJson[MessageError[Json]] =
    Argonaut.codec3(MessageError.apply[Json], (v: MessageError[Json]) => (v.code, v.message, v.data))(
      "codec",
      "message",
      "data"
    )

  implicit private lazy val messageCodecJson: CodecJson[Message[Json]] =
    Argonaut.codec6(Message.apply[Json], (v: Message[Json]) => (v.automorph, v.id, v.method, v.params, v.result, v.error))(
      "automorph",
      "id",
      "method",
      "params",
      "result",
      "error"
    )

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Json]): ArraySeq.ofByte =
    new ArraySeq.ofByte(message.asJson.nospaces.getBytes(charset))

  override def deserialize(data: ArraySeq.ofByte): Message[Json] =
    new String(data.unsafeArray, charset).decodeEither[Message[Json]].fold(
      errorMessage => throw new IllegalArgumentException(errorMessage),
      identity
    )

  override def format(message: Message[Json]): String =
    message.asJson.spaces2
}

case object ArgonautJsonCodec {

  implicit lazy val noneCodecJson: CodecJson[None.type] = CodecJson(
    (v: None.type) => jNull,
    cursor => if (cursor.focus.isNull) DecodeResult.ok(None) else DecodeResult.fail("Not a null", cursor.history)
  )
}
