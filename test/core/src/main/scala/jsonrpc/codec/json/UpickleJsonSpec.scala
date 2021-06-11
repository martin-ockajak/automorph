package jsonrpc.codec.json

import jsonrpc.codec.common.upickle.UpickleCustom
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{ClientHandlerSpec, ComplexApi, Enum, Handler, Record, Structure}
import ujson.Value

trait UpickleJsonSpec extends ClientHandlerSpec:

  type Node = Value
  type CodecType = UpickleJsonCodec[UpickleJsonSpec.type]

  def codec: CodecType = UpickleJsonCodec(UpickleJsonSpec)

  lazy val handler: Handler[Node, CodecType, Effect, Short] = Handler[Node, CodecType, Effect, Short](codec, backend)
    .bind(simpleApiInstance).bind[ComplexApi[Effect]](complexApiInstance)

  lazy val handlerTransport: HandlerTransport[Node, CodecType, Effect, Short] = HandlerTransport(handler, backend, 0)

object UpickleJsonSpec extends UpickleCustom:

  given ReadWriter[Enum] = readwriter[Int].bimap[Enum](
      value => value.ordinal,
      number => Enum.fromOrdinal(number)
    )
    given ReadWriter[Structure] = macroRW
    given ReadWriter[Record] = macroRW
