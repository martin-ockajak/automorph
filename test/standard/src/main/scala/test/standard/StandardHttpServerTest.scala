package test.standard

import automorph.spi.ClientTransport
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import test.core.ClientServerTest

trait StandardHttpServerTest extends ClientServerTest {

  override def clientTransport(id: Int): ClientTransport[Effect, ?] =
    HttpClient(system, url(id), HttpMethod.Post)

  def webSocket: Boolean =
    false
}
