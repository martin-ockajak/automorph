package jsonrpc.codec.json

import jsonrpc.codec.json.UpickleJsonSpec.{CodecType, Node, ReadWriters}
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.{ClientHandlerSpec, Enum, Record, Structure}
import ujson.Value
import upickle.AttributeTagged

trait UpickleJsonSpec[Effect[_]] extends ClientHandlerSpec[Value, CodecType, Effect]:
  def codec: CodecType = UpickleJsonCodec(ReadWriters)

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
