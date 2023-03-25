package test.core

import scala.collection.mutable
import test.base.{Await, Network}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  lazy val port: Int = acquireRandomPort

  override def afterAll(): Unit = {
    super.afterAll()
    releasePort(port)
  }

  private def acquireRandomPort: Int =
    ClientServerTest.usedPorts.synchronized {
      val port = randomPort
      if (ClientServerTest.usedPorts.contains(port)) {
        acquireRandomPort
      } else {
        ClientServerTest.usedPorts.add(port)
        port
      }
    }

  private def releasePort(port: Int): Unit =
    ClientServerTest.usedPorts.synchronized {
      ClientServerTest.usedPorts.remove(port)
    }
}

object ClientServerTest {
  private val usedPorts = mutable.HashSet[Int]()
}
