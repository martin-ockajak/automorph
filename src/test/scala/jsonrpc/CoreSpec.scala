//package jsonrpc
//
//import java.nio.charset.StandardCharsets
//import java.util.concurrent.LinkedTransferQueue
//import jsonrpc.{JsonRpcClient, JsonRpcMessage, JsonRpcServer}
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.{ExecutionContext, Future}
//
//class CoreSpec extends ClientServerSpec {
//  override def createClientServer(): (JsonRpcClient, JsonRpcServer) = {
//    val server = new BaseJsonRpcServer(jsonFormats, ExecutionContext.Implicits.global) {
//      override def close(): Unit = ()
//    }
//    (new DirectClient(server), server)
//  }
//
//  private class DirectClient(server: JsonRpcServer) extends BaseJsonRpcClient(jsonFormats, ExecutionContext.Implicits.global) {
//
//    override def sendCallRequest(requestMessage: Array[Byte]): Future[Array[Byte]] = {
//      sendRequest(requestMessage)
//    }
//
//    override def sendNotifyRequest(requestMessage: Array[Byte]): Future[Unit] = {
//      sendRequest(requestMessage).map(_ => ())
//    }
//
//    override def close(): Unit = ()
//
//    private def sendRequest(requestMessage: Array[Byte]): Future[Array[Byte]] = {
//      val responseQueue = new LinkedTransferQueue[Option[JsonRpcMessage]]
//      server.handleRequest(requestMessage, responseQueue.put, "Test Client")
//      Future(responseQueue.take().map(_.message.getBytes(StandardCharsets.UTF_8)).getOrElse(Array.empty))
//    }
//
//    override def getServerId: String = "Test Server"
//  }
//}
