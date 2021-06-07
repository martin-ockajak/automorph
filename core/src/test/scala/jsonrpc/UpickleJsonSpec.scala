package jsonrpc

import base.BaseSpec
import jsonrpc.UpickleJsonSpec.{CodecType, Node}
import jsonrpc.client.Client
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.spi.Codec
import jsonrpc.{ComplexApi, Enum, Record, SimpleApi, Structure}
import ujson.Value
import upickle.AttributeTagged

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
