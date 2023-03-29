package test.transport.local

import automorph.spi.{ClientTransport, ServerTransport}
import automorph.transport.local.LocalContext
import automorph.transport.local.client.LocalClient
import org.scalacheck.{Arbitrary, Gen}
import test.core.ClientServerTest

trait LocalTest extends ClientServerTest {

  type Context = LocalClient.Context

  private lazy val server = LocalServer(system)

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Gen.asciiPrintableStr.map(LocalContext.apply))

  override def clientTransport(id: Int): ClientTransport[Effect, ?] =
    LocalClient(system, handler = server.handler)

  override def serverTransport(id: Int): ServerTransport[Effect, Context] =
    server
}
