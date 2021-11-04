package automorph.transport.http

/** Transport protocol. */
sealed abstract private class Protocol(val name: String) {
  override def toString: String = name
}

/** Transport protocols. */
private object Protocol {

  case object Http extends Protocol("HTTP")

  case object WebSocket extends Protocol("WebSocket")
}
