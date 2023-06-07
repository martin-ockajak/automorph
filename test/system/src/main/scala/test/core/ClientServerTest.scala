package test.core

import java.net.URI
import scala.collection.mutable
import test.base.{Await, Network}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  private lazy val ports: mutable.Map[Int, Int] = mutable.HashMap()

  def port(fixtureId: Int): Int =
    ports.synchronized {
      ports.getOrElseUpdate(fixtureId, availablePort())
    }

  def url(fixtureId: Int): URI = {
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    new URI(s"$scheme://localhost:${port(fixtureId)}")
  }

  def webSocket: Boolean =
    false
}
