package automorph.codec.json

import automorph.protocol.jsonrpc.Message
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps
import automorph.spi.MessageCodec
import scala.compiletime.summonInline

/** Circe JSON codec plugin code generation. */
private[automorph] trait CirceJsonMeta extends MessageCodec[Json]:

  implicit private val messageEncoder: Encoder[Message[Json]] = CirceJsonRpc.messageEncoder

  override inline def encode[T](value: T): Json =
    import CirceJsonCodec.given
    value.asJson(using summonInline[Encoder[T]])

  override inline def decode[T](node: Json): T =
    import CirceJsonCodec.given
    node.as[T](using summonInline[Decoder[T]]).toTry.get
