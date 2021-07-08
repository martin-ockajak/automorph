package automorph

import automorph.codec.common.DefaultUpickleCustom
import automorph.codec.json.UpickleJsonCodec
import automorph.server.http.UndertowServer
import automorph.transport.http.SttpTransport.RequestProperties
import automorph.{Client, Handler}
import ujson.Value

case object DefaultTypes {

  type DefaultClient[Effect[_]] =
    Client[Value, UpickleJsonCodec[DefaultUpickleCustom.type], Effect, RequestProperties]

  type DefaultHandler[Effect[_], Context] = Handler[Value, UpickleJsonCodec[DefaultUpickleCustom.type], Effect, Context]

  type DefaultServer = UndertowServer
}
