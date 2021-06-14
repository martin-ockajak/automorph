package jsonrpc.codec.json

import jsonrpc.Enum.Enum
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.{ClientHandlerSpec, ComplexApi, Handler, Record, Structure}
import ujson.Value

trait UpickleJsonSpec extends ClientHandlerSpec {

  type Node = Value
  type CodecType = UpickleJsonCodec[UpickleJsonSpec.type]

  def codec: CodecType = UpickleJsonCodec(UpickleJsonSpec)

  lazy val handler: Handler[Node, CodecType, Effect, Context] = Handler[Node, CodecType, Effect, Context](codec, backend)
    .bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)

  lazy val handlerTransport: HandlerTransport[Node, CodecType, Effect, Context] = HandlerTransport(handler, backend, arbitraryContext.arbitrary.sample.get)
}

object UpickleJsonSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum] = readwriter[Int].bimap[Enum](
    value => value.ordinal,
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
