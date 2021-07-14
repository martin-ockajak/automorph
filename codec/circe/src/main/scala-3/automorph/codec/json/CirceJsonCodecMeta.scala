package automorph.codec.json

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps
import automorph.spi.MessageFormat
import scala.compiletime.summonInline

/**
 * Circe JSON codec plugin code generation.
 */
private[automorph] trait CirceJsonCodecMeta extends MessageFormat[Json]:

  override inline def encode[T](value: T): Json =
    value.asJson(using summonInline[Encoder[T]])

  override inline def decode[T](node: Json): T =
    node.as[T](using summonInline[Decoder[T]]).toTry.get
