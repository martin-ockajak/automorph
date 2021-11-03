package test.base

import java.net.ServerSocket

trait Network {
  /**
   * Determine random available network port.
   *
   * @return port number
   */
  def randomAvailablePort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }

  /**
   * Determine random available network port and pass it to a function.
   *
   * Different invocations of this method are executed in a mutually exclusive manner.
   *
   * @param function function supplied with available port number
   * @return function result
   */
  def withRandomAvailablePort[T](function: Int => T): T = Network.synchronized {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    function(port)
  }
}

object Network
