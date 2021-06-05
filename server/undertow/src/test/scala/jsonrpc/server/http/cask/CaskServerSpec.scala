//package jsonrpc.server.http.cask
//
//import base.BaseSpec
//import io.undertow.server.{HttpHandler, HttpServerExchange}
//import io.undertow.{Handlers, Undertow}
//import io.undertow.server.handlers.BlockingHandler
//import jsonrpc.JsonRpcHandler
//import jsonrpc.backend.standard.NoBackend
//import jsonrpc.codec.json.upickle.UpickleJsonCodec
//import jsonrpc.http.undertow.UndertowJsonRpcHandler
//import scala.concurrent.Future
//import scala.language.adhocExtensions
//
//class CaskServerSpec extends BaseSpec {
//
//}
//
//object Api:
//  def test(value: String)(using exchange: HttpServerExchange): String = value
//
//object CaskServer extends cask.MainRoutes:
//  val apiPath = "/api"
//
//  override def defaultHandler =
//    val httpHandler = UndertowJsonRpcHandler(
//      JsonRpcHandler(UpickleJsonCodec(), NoBackend()).bind(Api),
//      _ => ()
//    )
//    val pathHandler = Handlers.path(super.defaultHandler).addPrefixPath(apiPath, httpHandler)
//    BlockingHandler(pathHandler)
//
//  @cask.get("/")
//  def hello() = "Hello World!"
//
//  initialize()
