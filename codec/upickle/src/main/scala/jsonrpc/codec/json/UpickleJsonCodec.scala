package jsonrpc.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Message
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

  private val indent = 2

  override def mediaType: String = "application/json"

  override def serialize(message: Message[Value]): ArraySeq.ofByte = {
    implicit val writer = custom.jsonMessageRw
    new ArraySeq.ofByte(custom.writeToByteArray(JsonMessage.fromSpi(message)))
  }

  override def deserialize(data: ArraySeq.ofByte): Message[Value] = {
    implicit val reader = custom.jsonMessageRw
    custom.read[JsonMessage](data.unsafeArray).toSpi
  }

  override def format(message: Message[Value]): String = {
    implicit val writer = custom.jsonMessageRw
    custom.write(JsonMessage.fromSpi(message), indent)
  }
}
