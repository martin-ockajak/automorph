package test.core

import java.net.URI
import scala.collection.mutable
import test.base.{Await, Network}

trait ClientServerTest extends ProtocolCodecTest with Await with Network {
  private lazy val ports: mutable.Map[Int, Int] = mutable.HashMap()

  def port(fixtureId: Int): Int = {
    val objectId = System.identityHashCode(this)
    ports.synchronized {
      val inCache = ports.isDefinedAt(fixtureId)
      val port = ports.getOrElseUpdate(fixtureId, claimPort())
      val source = if (inCache) s"from cache($port)" else s"newly locked port ($port)"
      println(s"$ClientServerTest@$objectId: asking for port(fixtureId: $fixtureId) -> $source")
      port
    }
  }

  def url(fixtureId: Int): URI = {
    val objectId = System.identityHashCode(this)
    println(s"$ClientServerTest@$objectId: asking for url  (fixtureId: $fixtureId)")
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    new URI(s"$scheme://localhost:${port(fixtureId)}")
  }

  def webSocket: Boolean =
    false
}

case object ClientServerTest