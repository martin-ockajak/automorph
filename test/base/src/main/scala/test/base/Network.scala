package test.base

import java.net.ServerSocket

trait Network {

  /**
   * Determine random available network port.
   *
   * @return
   *   port number
   */
  def randomPort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}
