package automorph.format.json

import argonaut.Argonaut.ToJsonIdentity
import argonaut.{CodecJson, DecodeJson, EncodeJson, Json}
import automorph.spi.MessageFormat
import scala.compiletime.summonInline

/** Argonaut JSON format plugin code generation. */
private[automorph] trait ArgonautJsonMeta extends MessageFormat[Json]:

  override inline def encode[T](value: T): Json =
    import ArgonautJsonFormat.noneCodecJson
    value.asJson(using summonInline[EncodeJson[T]])

  override inline def decode[T](node: Json): T =
    import ArgonautJsonFormat.noneCodecJson
    node.as[T](using summonInline[DecodeJson[T]]).fold(
      (errorMessage, _) => throw new IllegalArgumentException(errorMessage),
      identity
    )
