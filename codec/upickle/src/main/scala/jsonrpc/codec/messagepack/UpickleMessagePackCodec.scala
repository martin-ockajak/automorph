package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
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

  private val indent = 2

  override def mediaType: String = "application/msgpack"

  override def serialize(message: Message[Msg]): ArraySeq.ofByte = {
    implicit val writer = custom.messagePackMessageRw
    new ArraySeq.ofByte(custom.writeToByteArray(MessagePackMessage.fromSpi(message)))
  }

  override def deserialize(data: ArraySeq.ofByte): Message[Msg] = {
    implicit val reader = custom.messagePackMessageRw
    custom.read[MessagePackMessage](data.unsafeArray).toSpi
  }

  override def format(message: Message[Msg]): String = {
    implicit val writer = custom.messagePackMessageRw
    custom.write(MessagePackMessage.fromSpi(message), indent)
  }
}
