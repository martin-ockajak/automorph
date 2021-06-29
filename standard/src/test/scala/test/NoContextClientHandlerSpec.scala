package test

import base.BaseSpec
import jsonrpc.backend.NoBackend
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.transport.local.HandlerTransport
import jsonrpc.util.Void
import jsonrpc.{Client, Handler}

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
