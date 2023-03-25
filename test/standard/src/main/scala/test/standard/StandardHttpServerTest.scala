package test.standard

import automorph.spi.ClientTransport
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import java.net.URI
import test.core.ClientServerTest

trait StandardHttpServerTest extends ClientServerTest {

  override def clientTransport: ClientTransport[Effect, ?] = {
    val scheme = Option.when(webSocket)("ws").getOrElse("http")
    HttpClient(system, new URI(s"$scheme://localhost:$port"), HttpMethod.Post)
  }

  def webSocket: Boolean =
    false
}
