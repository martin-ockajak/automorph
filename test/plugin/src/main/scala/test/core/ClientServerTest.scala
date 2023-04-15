package test.core

import java.net.URI
import scala.collection.mutable
import test.base.{Await, Network}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  private lazy val ports: mutable.Map[Int, Int] = mutable.HashMap()

  def port(id: Int): Int =
    ports.synchronized {
      ports.getOrElseUpdate(id, acquirePort)
    }

  def url(id: Int): URI = {
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    new URI(s"$scheme://localhost:${port(id)}")
  }

  def portRange: Range =
    Range(30000, 64000)

  def webSocket: Boolean =
    false

  override def afterAll(): Unit = {
    super.afterAll()
    ports.synchronized {
      ports.values.foreach(releasePort)
    }
  }

  private def acquirePort: Int =
    ClientServerTest.usedPorts.synchronized {
      val port = availablePort(portRange, ClientServerTest.usedPorts.toSet)
      if (ClientServerTest.usedPorts.contains(port)) {
        acquirePort
      } else {
        ClientServerTest.usedPorts.add(port)
        port
      }
    }

  private def releasePort(port: Int): Unit =
    ClientServerTest.usedPorts.synchronized {
      ClientServerTest.usedPorts.remove(port)
      ()
    }
}

case object ClientServerTest {
  private val usedPorts = mutable.HashSet[Int]()
}
