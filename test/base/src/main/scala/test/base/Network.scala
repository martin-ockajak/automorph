package test.base

import java.net.ServerSocket
import java.nio.file.{Files, Path, Paths}
import scala.util.Try

trait Network {
  private lazy val minPort = 10000
  private lazy val maxPort = 65535

  def availablePort(excluded: Set[Int]): Int =
    LazyList.from(minPort).takeWhile(_ <= maxPort).filterNot(excluded.contains).find { port =>
      val lockFile = Network.portLockDirectory.resolve(f"port-$port%05d.lock").toFile
      lockFile.createNewFile() && {
        lockFile.deleteOnExit()
        portAvailable(port)
      }
    }.getOrElse(throw new IllegalStateException("No available ports found"))

  private def portAvailable(port: Int): Boolean =
    Try(new ServerSocket(port)).map(_.close()).isSuccess
}

case object Network {

  private lazy val portLockDirectory: Path = {
    val projectDir = Paths.get("")
    val targetDir = projectDir.resolve("target")
    if (!Files.exists(targetDir)) {
      throw new IllegalStateException(s"Project directory does not contain a target directory: $projectDir")
    }
    val portLockDir = targetDir.resolve("port.lock")
    Files.createDirectories(portLockDir)
    portLockDir
  }
}
