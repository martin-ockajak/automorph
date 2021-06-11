package base

import java.net.ServerSocket

trait Network:
  /**
   * Determine available network port.
   *
   * @return port number
   */
  def availablePort: Int =
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
