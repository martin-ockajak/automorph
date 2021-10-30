package test.transport.http

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import automorph.transport.http.client.HttpClient
import java.net.URI
import org.scalacheck.Arbitrary
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import test.core.ClientServerTest
import test.transport.http.HttpContextGenerator

trait StandardHttpServerTest extends ClientServerTest {

  override lazy val system: EffectSystem[Effect] = deferSystem

  def deferSystem: EffectSystem[Effect] with Defer[Effect]

  def serverTransport(port: Int): (ServerMessageTransport[Effect], Int)

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val (server, port) = withAvailablePort(serverTransport)
    servers += server
    val url = new URI(s"http://localhost:$port")
    val client = HttpClient.create(url, "POST", deferSystem)(runEffect)
      .asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}
