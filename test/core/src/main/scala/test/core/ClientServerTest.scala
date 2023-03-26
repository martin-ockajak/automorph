package test.core

import scala.collection.mutable
import test.base.{Await, Network}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  lazy val ports: Seq[Int] = fixtures.map(_ => acquireRandomPort)

  override def afterAll(): Unit = {
    super.afterAll()
    ports.foreach(releasePort)
  }

  private def acquireRandomPort: Int =
    ClientServerTest.usedPorts.synchronized {
      val port = randomPort
      if (ClientServerTest.usedPorts.contains(port)) {
        acquireRandomPort
      } else {
        ClientServerTest.usedPorts.add(port)
//        println(ClientServerTest.usedPorts)
//        println(port)
//        println()
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
