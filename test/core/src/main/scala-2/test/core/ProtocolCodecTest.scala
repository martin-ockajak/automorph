package test.core

import automorph.spi.transport.ClientMessageTransport
import automorph.Types
import scala.annotation.nowarn

trait ProtocolCodecTest extends CoreTest {

  private lazy val testFixtures: Seq[TestFixture] = Seq()

  override def fixtures: Seq[TestFixture] =
    testFixtures

  @nowarn("msg=used")
  def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] =
    None
}
