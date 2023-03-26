package test.transport.local

import automorph.spi.{ClientTransport, EffectSystem, RequestHandler, ServerTransport}
import automorph.transport.local.LocalContext
import automorph.transport.local.client.LocalClient
import automorph.transport.local.endpoint.LocalEndpoint
import org.scalacheck.{Arbitrary, Gen}
import test.core.ClientServerTest

trait LocalTest extends ClientServerTest {

  type Context = LocalClient.Context

  private lazy val server = LocalServer(system)

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Gen.asciiPrintableStr.map(LocalContext.apply))

  override def clientTransport(index: Int): ClientTransport[Effect, ?] =
    LocalClient(system, handler = server.handler)

  override def serverTransport(index: Int): ServerTransport[Effect, Context] =
    server

  private final case class LocalServer(effectSystem: EffectSystem[Effect]) extends ServerTransport[Effect, Context] {
    private var endpoint = LocalEndpoint(effectSystem)

    def handler: RequestHandler[Effect, Context] =
      endpoint.handler

    override def clone(handler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] = {
      endpoint = endpoint.clone(handler)
      this
    }

    override def init(): Effect[Unit] =
      system.successful(())

    override def close(): Effect[Unit] =
      system.successful(())
  }
}
