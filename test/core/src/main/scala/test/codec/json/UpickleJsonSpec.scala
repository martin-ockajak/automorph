package test.codec.json

import jsonrpc.Handler
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.transport.local.HandlerTransport
import test.{ClientHandlerSpec, ComplexApi, Enum, Record, Structure}
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

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
