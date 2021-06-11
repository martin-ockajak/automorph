package jsonrpc.codec.json

import jsonrpc.Handler
import jsonrpc.spi.Backend
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{ClientHandlerSpec, ComplexApi, Enum, Record, Structure}
import ujson.Value
import upickle.AttributeTagged

trait UpickleJsonSpec extends ClientHandlerSpec:

  type Node = Value
  type CodecType = UpickleJsonCodec[UpickleJsonSpec.type]

  def codec: CodecType = UpickleJsonCodec(UpickleJsonSpec)

  lazy val handler: Handler[Node, CodecType, Effect, Short] = Handler[Node, CodecType, Effect, Short](codec, backend)
    .bind(simpleApiInstance).bind[ComplexApi[Effect]](complexApiInstance)

  lazy val handlerTransport: HandlerTransport[Node, CodecType, Effect, Short] = HandlerTransport(handler, backend, 0)

object UpickleJsonSpec extends AttributeTagged:

    given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
      value => value.ordinal,
      number => Enum.fromOrdinal(number)
    )
    given ReadWriter[Structure] = macroRW
    given ReadWriter[Record] = macroRW
