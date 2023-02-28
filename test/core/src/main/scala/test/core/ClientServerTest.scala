package test.core

import automorph.spi.{ClientTransport, EffectSystem, ServerTransport}
import scala.collection.mutable.ArrayBuffer

trait ClientServerTest extends ProtocolCodecTest {

  def system: EffectSystem[Effect]

  lazy val servers: ArrayBuffer[ServerTransport[Effect, Context]] = ArrayBuffer.empty

  lazy val clients: ArrayBuffer[ClientTransport[Effect, Context]] = ArrayBuffer.empty

  override def afterAll(): Unit = {
    servers.foreach(server => system.runAsync(server.close()))
    clients.foreach(client => system.runAsync(client.close()))
    super.afterAll()
  }
}
