package test.transport.http

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity

class HttpClientHttpIdentityTest extends HttpClientHttpTest {

  type Effect[T] = Identity[T]

  override lazy val system: IdentitySystem = IdentitySystem()

  override def run[T](effect: Effect[T]): T =
    effect
}
