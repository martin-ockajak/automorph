package jsonrpc

import base.BaseSpec
import jsonrpc.client.{Client, ClientFactory}
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.handler.{Handler, HandlerFactory}
import jsonrpc.spi.{Backend, Codec, Transport}
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.UpickleJsonSpec.CodecType
import jsonrpc.UpickleJsonSpec.Node
import jsonrpc.{ComplexApi, Enum, Record, SimpleApi, Structure}
import ujson.Value
import upickle.AttributeTagged

trait UpickleJsonSpec[Effect[_]] extends CoreSpec[Node, CodecType, Effect]:

  def codec: CodecType = UpickleJsonCodec(ReadWriters)

  def backend: Backend[Effect]

  inline def transport: Transport[Effect, Short] = HandlerTransport(handler, backend)

  inline def client: Client[Node, CodecType, Effect, Short] =
    ClientFactory(codec, backend, transport)

  inline def handler: Handler[Node, UpickleJsonCodec[ReadWriters.type], Effect, Short] =
    HandlerFactory(codec, backend).bind(simpleApi).bind[ComplexApi[Effect]](complexApi)

  inline def simpleApiProxy: SimpleApi[Effect] = client.bind[SimpleApi[Effect]]

  inline def complexApiProxy: ComplexApi[Effect] = client.bind[ComplexApi[Effect]]

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
