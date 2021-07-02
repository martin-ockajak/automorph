package test.server.http

import base.BaseSpec
import io.undertow.Handlers
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.BlockingHandler
import jsonrpc.Handler
import jsonrpc.backend.IdentityBackend
import jsonrpc.backend.IdentityBackend.Identity
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
    val handler = Handler[Value, codec.type, Identity, HttpServerExchange](codec, IdentityBackend()).bind(Api)
    val httpHandler = UndertowJsonRpcHandler[Value, codec.type, Identity](handler, (_: Any) => ())
    val pathHandler = Handlers.path(super.defaultHandler).addPrefixPath(apiPath, httpHandler)
    new BlockingHandler(pathHandler)
  }

  initialize()
}
