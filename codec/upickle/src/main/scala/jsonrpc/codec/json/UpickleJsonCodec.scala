package jsonrpc.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Message
import jsonrpc.codec.json
import scala.collection.immutable.ArraySeq
import ujson.Value

/**
 * UPickle JSON codec plugin.
 *
 * @see [[https://github.com/com-lihaoyi/upickle Documentation]]
 * @see [[http://com-lihaoyi.github.io/upickle/#uJson Node type]]
 * @param custom customized Upickle reader and writer implicits instance
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
final case class UpickleJsonCodec[Custom <: UpickleCustom](
  custom: Custom = new UpickleCustom {}
) extends UpickleJsonCodecMeta[Custom] {
  import custom._

  private val indent = 2
  private implicit val jsonMmessageErrorRw: custom.ReadWriter[json.UpickleMessageError] = custom.macroRW
  private implicit val jsonMessageRw: custom.ReadWriter[json.UpickleMessage] = custom.macroRW
  jsonMmessageErrorRw

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Value]): ArraySeq.ofByte = {
    new ArraySeq.ofByte(custom.writeToByteArray(UpickleMessage.fromSpi(message)))
  }

  override def deserialize(data: ArraySeq.ofByte): Message[Value] = {
    custom.read[UpickleMessage](data.unsafeArray).toSpi
  }

  override def format(message: Message[Value]): String = {
    custom.write(UpickleMessage.fromSpi(message), indent)
  }
}
