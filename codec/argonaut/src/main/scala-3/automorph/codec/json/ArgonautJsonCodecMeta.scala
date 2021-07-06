package automorph.codec.json

import argonaut.Argonaut.ToJsonIdentity
import argonaut.{CodecJson, DecodeJson, EncodeJson, Json}
import automorph.spi.Codec
import scala.compiletime.summonInline

/** Argonaut JSON codec plugin code generation. */
private[automorph] trait ArgonautJsonCodecMeta extends Codec[Json]:

  override inline def encode[T](value: T): Json =
    value.asJson(using summonInline[EncodeJson[T]])

  override inline def decode[T](node: Json): T =
    node.as[T](using summonInline[DecodeJson[T]]).fold(
      (errorMessage, _) => throw new IllegalArgumentException(errorMessage),
      identity
    )
