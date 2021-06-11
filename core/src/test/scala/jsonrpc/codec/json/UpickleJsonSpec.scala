package jsonrpc.codec.json

import jsonrpc.codec.json.UpickleJsonSpec.{CodecType, Node, ReadWriters}
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.Handler
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{ClientHandlerSpec, ComplexApi, Enum, Record, Structure}
import ujson.Value
import upickle.AttributeTagged

trait UpickleJsonSpec[Effect[_]] extends ClientHandlerSpec[Value, CodecType, Effect]:

  def codec: CodecType = UpickleJsonCodec(ReadWriters)

  def backend: Backend[Effect]

  lazy val handler: Handler[Node, CodecType, Effect, Short] = Handler[Node, CodecType, Effect, Short](codec, backend)
    .bind(simpleApiInstance).bind[ComplexApi[Effect]](complexApiInstance)

  lazy val handlerTransport: HandlerTransport[Node, CodecType, Effect, Short] = HandlerTransport(handler, backend, 0)

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
