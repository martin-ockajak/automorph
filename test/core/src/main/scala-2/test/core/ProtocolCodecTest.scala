package test.core

import automorph.spi.transport.ClientMessageTransport
import automorph.Types
import scala.annotation.nowarn
import test.core.CoreTest

trait ProtocolCodecTest extends CoreTest {

  private lazy val testFixtures: Seq[TestFixture] = Seq()

  override def fixtures: Seq[TestFixture] =
    testFixtures

  @nowarn("msg=used")
  def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] =
    None
}
