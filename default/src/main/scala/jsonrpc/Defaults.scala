package jsonrpc

import jsonrpc.Handler
import jsonrpc.codec.common.UpickleCustom
import jsonrpc.codec.json.UpickleJsonCodec
import ujson.Value

case object Defaults {

  type DefaultHandler[Effect[_], Context] = Handler[Value, UpickleJsonCodec[UpickleCustom], Effect, Context]

}
