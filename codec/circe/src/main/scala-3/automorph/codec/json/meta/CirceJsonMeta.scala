package automorph.codec.json.meta

import automorph.spi.MessageCodec
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import scala.compiletime.summonInline

/** Circe JSON codec plugin code generation. */
private[automorph] trait CirceJsonMeta extends MessageCodec[Json]:

  override inline def encode[T](value: T): Json =(using summonInline[Encoder[T]])

  override inline def decode[T](node: Json): T =(using summonInline[Decoder[T]]).toTry.get
