package test.core

import automorph.spi.EffectSystem
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import scala.collection.mutable.ArrayBuffer
import test.core.ProtocolCodecTest

trait ClientServerTest extends ProtocolCodecTest {

  lazy val servers: ArrayBuffer[ServerMessageTransport[Effect]] = ArrayBuffer.empty

  lazy val clients: ArrayBuffer[ClientMessageTransport[Effect, Context]] = ArrayBuffer.empty

  def runEffect[T](effect: Effect[T]): Unit

  override def afterAll(): Unit = {
    servers.foreach(server => runEffect(server.close()))
    clients.foreach(client => runEffect(client.close()))
    super.afterAll()
  }
}
