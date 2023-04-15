package test.base

import java.net.ServerSocket
import scala.util.Try

trait Network {

  /**
   * Determine first available network port from a specified range.
   *
   * @param range
   *   port range
   * @param excluded
   *   excluded ports
   * @return
   *   port number
   */
  def availablePort(range: Range, excluded: Set[Int]): Int =
    range.filterNot(excluded.contains).find { port =>
      Try(new ServerSocket(port)).map(_.close()).isSuccess
    }.getOrElse(throw new IllegalStateException("No ports available"))

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
