package test.core

import java.net.URI
import scala.collection.mutable
import test.base.{Await, Network}
import test.core.ClientServerTest.usedPorts

import scala.collection.immutable.{SortedMap, SortedSet}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  private lazy val ports: mutable.Map[Int, Int] = mutable.HashMap()

  def port(id: Int): Int =
    ports.getOrElseUpdate(id, acquirePort)

  def url(id: Int): URI = {
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    new URI(s"$scheme://localhost:${port(id)}")
  }

  def webSocket: Boolean =
    false

  private def acquirePort: Int =
    ClientServerTest.usedPorts.synchronized {
      val port = availablePort(usedPorts.toSet)
      ClientServerTest.usedPorts.add(port)
      port
    }
}

case object ClientServerTest {
  private val usedPorts = mutable.Set.empty[Int]
}
