package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.messagepack
import jsonrpc.spi.Message
import scala.collection.immutable.ArraySeq
import upack.Msg

/**
 * UPickle MessagePack codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uPack Node type]]
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleMessagePackCodec[Custom <: UpickleCustom](
  custom: Custom = new UpickleCustom {}
) extends UpickleMessagePackCodecMeta[Custom] {
  import custom._

  private val indent = 2
  private implicit val messagePackMessageErrorRw: custom.ReadWriter[messagepack.UpickleMessageError] = custom.macroRW
  private implicit val messagePackMessageRw: custom.ReadWriter[messagepack.UpickleMessage] = custom.macroRW
  Seq(messagePackMessageErrorRw)

  override def mediaType: String = "application/msgpack"

  override def serialize(message: Message[Msg]): ArraySeq.ofByte =
    new ArraySeq.ofByte(custom.writeToByteArray(UpickleMessage.fromSpi(message)))

  override def deserialize(data: ArraySeq.ofByte): Message[Msg] =
    custom.read[UpickleMessage](data.unsafeArray).toSpi

  override def format(message: Message[Msg]): String =
    custom.write(UpickleMessage.fromSpi(message), indent)
}
