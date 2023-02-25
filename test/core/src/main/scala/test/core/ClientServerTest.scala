package test.core

import automorph.spi.EffectSystem
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import scala.collection.mutable.ArrayBuffer

trait ClientServerTest extends ProtocolCodecTest {

  def system: EffectSystem[Effect]

  lazy val servers: ArrayBuffer[ServerMessageTransport[Effect, Context]] = ArrayBuffer.empty

  lazy val clients: ArrayBuffer[ClientMessageTransport[Effect, Context]] = ArrayBuffer.empty

  override def afterAll(): Unit = {
    servers.foreach(server => system.runAsync(server.close()))
    clients.foreach(client => system.runAsync(client.close()))
    super.afterAll()
  }
}
