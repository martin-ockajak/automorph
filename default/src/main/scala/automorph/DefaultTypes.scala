package automorph

import automorph.backend.IdentityBackend.Identity
import automorph.codec.common.DefaultUpickleCustom
import automorph.codec.json.UpickleJsonCodec
import automorph.server.http.UndertowServer
import automorph.spi.{Backend, Transport}
import automorph.transport.http.SttpTransport.RequestProperties
import automorph.{Client, Handler}
import scala.concurrent.Future
import ujson.Value

case object DefaultTypes {

  type DefaultAsyncBackend = Backend[Future]

  type DefaultSyncBackend = Backend[Identity]

  type DefaultNode = Value

  type DefaultCodec = UpickleJsonCodec[DefaultUpickleCustom.type]

  type DefaultTransport[Effect[_]] = Transport[Effect, RequestProperties]

  type DefaultClient[Effect[_]] = Client[DefaultNode, DefaultCodec, Effect, RequestProperties]

  type DefaultHandler[Effect[_], Context] = Handler[DefaultNode, DefaultCodec, Effect, Context]

  type DefaultServer = UndertowServer
}
