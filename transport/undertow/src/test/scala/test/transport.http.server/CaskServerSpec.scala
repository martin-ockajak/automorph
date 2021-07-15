package test.transport.http.server

import io.undertow.Handlers
import io.undertow.server.handlers.BlockingHandler
import automorph.Handler
import automorph.codec.json.UpickleJsonFormat
import automorph.transport.http.endpoint.UndertowHandlerEndpoint
import automorph.system.IdentityBackend
import automorph.system.IdentityBackend.Identity
import test.base.BaseSpec
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

  override def defaultHandler: BlockingHandler = {
    val codec = UpickleJsonFormat()
    val httpHandler = UndertowHandlerEndpoint(Handler(codec, IdentityBackend()).bind(Api), (_: Any) => ())
    val pathHandler = Handlers.path(super.defaultHandler).addPrefixPath(apiPath, httpHandler)
    new BlockingHandler(pathHandler)
  }

  initialize()
}
