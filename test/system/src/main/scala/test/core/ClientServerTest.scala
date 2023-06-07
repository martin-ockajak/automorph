package test.core

import java.net.URI
import scala.collection.mutable
import test.base.{Await, Network}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  private lazy val ports: mutable.Map[Int, Int] = mutable.HashMap()

  def port(fixtureId: Int): Int =
    ports.getOrElseUpdate(fixtureId, acquirePort)

  def url(fixtureId: Int): URI = {
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    new URI(s"$scheme://localhost:${port(fixtureId)}")
  }

  def webSocket: Boolean =
    false

  private def acquirePort: Int =
    ClientServerTest.synchronized {
      println(s"Acquiring port: ${this.getClass.getSimpleName}")
      val port = availablePort()
      println(s"Acquired port: ${this.getClass.getSimpleName} - $port")
      port
    }
}

case object ClientServerTest {
  private val usedPorts = mutable.HashSet[Int]()
}
