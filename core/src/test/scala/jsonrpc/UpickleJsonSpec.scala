package jsonrpc

import base.BaseSpec
import jsonrpc.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.spi.Codec
import jsonrpc.{ComplexApi, Enum, Record, SimpleApiImpl, Structure}
import ujson.Value
import upickle.AttributeTagged

//class UpickleJsonSpec[Effect[_]] extends CoreSpec[Value, CodecType, Effect]:
//  def codec: CodecType = UpickleJsonCodec(ReadWriters)

object UpickleJsonSpec:
  type Node = Value
  type CodecType = UpickleJsonCodec[ReadWriters.type]

object ReadWriters extends AttributeTagged:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  given ReadWriter[Structure] = macroRW
  given ReadWriter[Record] = macroRW
