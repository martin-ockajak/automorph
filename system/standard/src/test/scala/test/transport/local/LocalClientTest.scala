package test.transport.local

import automorph.spi.{ClientTransport, ServerTransport}
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest

class LocalClientTest extends ProtocolCodecTest {

  type Context = Map[String, Double]

  override def clientTransport: ClientTransport[Effect, ?] = {
    LocalClient(system, , arbitraryContext.arbitrary.sample.get)
  }

  def serverTransport: ServerTransport[Effect, Context] =
    ???

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
