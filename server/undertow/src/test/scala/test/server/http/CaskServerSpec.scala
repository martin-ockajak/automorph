package test.server.http

import base.BaseSpec
import io.undertow.Handlers
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
import jsonrpc.Handler
import jsonrpc.backend.NoBackend
import jsonrpc.backend.NoBackend.Identity
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import jsonrpc.server.http.UndertowJsonRpcHandler
import ujson.Value

class CaskServerSpec extends BaseSpec {
  "" - {
    "Server" in {
      CaskServer
    }
  }
}

object Api {
  def test(value: String): String = value
}

object CaskServer extends cask.MainRoutes {
  val apiPath = "/api"

  override def defaultHandler = {
    val codec = UpickleJsonCodec[UpickleCustom]()
    val httpHandler = UndertowJsonRpcHandler(
      Handler[Value, codec.type, Identity, HttpServerExchange](codec, NoBackend()).bind(Api),
      (_: Any) => ()
    )
    val pathHandler = Handlers.path(super.defaultHandler).addPrefixPath(apiPath, httpHandler)
    new BlockingHandler(pathHandler)
  }

  initialize()
}
