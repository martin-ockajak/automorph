package automorph.transport.http.server

import automorph.spi.EffectSystem
import automorph.transport.http.server.NanoHTTPD.AsyncRunner
import automorph.util.Extensions.EffectOps
import scala.collection.concurrent.TrieMap

final case class AsyncEffectRunner[Effect[_]](
  system: EffectSystem[Effect]
) extends AsyncRunner {

  implicit private val givenSystem: EffectSystem[Effect] = system

  private val handlers: TrieMap[NanoHTTPD#ClientHandler, Unit] = TrieMap.empty

  override def exec(handler: NanoHTTPD#ClientHandler): Unit = {
    handlers += handler -> ()
    system.wrap(handler.run()).run
  }

  override def closed(handler: NanoHTTPD#ClientHandler): Unit =
    handlers -= handler

  override def closeAll(): Unit =
    handlers.keys.foreach(_.close())
}
