package test.codec.json

import jsonrpc.Handler
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.transport.local.HandlerTransport
import test.{ClientHandlerSpec, ComplexApi, Enum, InvalidApi, Record, SimpleApi, Structure}
import ujson.Value

trait UpickleJsonSpec extends ClientHandlerSpec {

  type Node = Value
  type CodecType = UpickleJsonCodec[UpickleJsonSpec.type]

  def handler: Handler[Node, CodecType, Effect, Context]

  lazy val codec: CodecType = UpickleJsonCodec(UpickleJsonSpec)

  lazy val handlerTransport: HandlerTransport[Node, CodecType, Effect, Context] = {
    val boundHandler = handler.bind(simpleApiInstance).bind[ComplexApi[Effect, Context]](complexApiInstance)
    HandlerTransport(boundHandler, backend, arbitraryContext.arbitrary.sample.get)
  }

  override def simpleApis: Seq[SimpleApi[Effect]] = clients.map(_.bind[SimpleApi[Effect]])

  override def complexApis: Seq[ComplexApi[Effect, Context]] = clients.map(_.bind[ComplexApi[Effect, Context]])

  override def invalidApis: Seq[InvalidApi[Effect]] = clients.map(_.bind[InvalidApi[Effect]])
}

object UpickleJsonSpec extends UpickleCustom {

  implicit def enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
    value => Enum.toOrdinal(value),
    number => Enum.fromOrdinal(number)
  )
  implicit def structureRw: ReadWriter[Structure] = macroRW
  implicit def recordRw: ReadWriter[Record] = macroRW
}
