package jsonrpc

import base.BaseSpec
import jsonrpc.backend.standard.NoBackend
import jsonrpc.backend.standard.NoBackend.Identity
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.{UpickleJsonCodec, UpickleJsonSpec}
import jsonrpc.spi.Transport
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.util.Void
import scala.collection.immutable.ArraySeq

class NoContextClientHandlerSpec extends BaseSpec {

  "" - {
    "Construct" in {
      val codec = UpickleJsonCodec[UpickleCustom]()
      val backend = NoBackend()
      val handler = Handler.basic(codec, backend)
      val handlerTransport = HandlerTransport(handler, backend, Void.value)
      Client.basic(codec, backend, handlerTransport)
    }
  }
}
