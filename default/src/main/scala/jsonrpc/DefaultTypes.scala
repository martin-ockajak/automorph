package jsonrpc

import jsonrpc.{Client, Handler}
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import sttp.client3.PartialRequest
import ujson.Value

case object DefaultTypes {

  type DefaultHandler[Effect[_], Context] = Handler[Value, UpickleJsonCodec[UpickleCustom], Effect, Context]

  type DefaultClient[Effect[_]] =
    Client[Value, UpickleJsonCodec[UpickleCustom], Effect, PartialRequest[Either[String, String], Any]]
}
