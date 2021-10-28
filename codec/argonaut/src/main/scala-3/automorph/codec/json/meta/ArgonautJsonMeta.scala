package automorph.codec.json.meta

import argonaut.Argonaut.ToJsonIdentity
import argonaut.{CodecJson, DecodeJson, EncodeJson, Json}
import automorph.spi.MessageCodec
import scala.compiletime.summonInline

/** Argonaut JSON codec plugin code generation. */
private[automorph] trait ArgonautJsonMeta extends MessageCodec[Json]:

  override inline def encode[T](value: T): Json =(using summonInline[EncodeJson[T]])

  override inline def decode[T](node: Json): T =(using summonInline[DecodeJson[T]]).fold(
      (errorMessage, _) => throw new IllegalArgumentException(errorMessage),
      identity
    )
