package automorph

import automorph.codec.common.UpickleCustom
import automorph.codec.json.UpickleJsonCodec
import automorph.server.http.UndertowServer
import automorph.transport.http.SttpTransport.HttpProperties
import automorph.{Client, Handler}
import ujson.Value

case object DefaultTypes {

  type DefaultClient[Effect[_]] =
    Client[Value, UpickleJsonCodec[UpickleCustom], Effect, HttpProperties]

  type DefaultHandler[Effect[_], Context] = Handler[Value, UpickleJsonCodec[UpickleCustom], Effect, Context]

  type DefaultServer = UndertowServer
}
