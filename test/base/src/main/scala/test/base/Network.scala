package test.base

import java.net.ServerSocket
import java.nio.file.{Files, Path, Paths}
import scala.util.Try

trait Network {
  private lazy val minPort = 16384
  private lazy val maxPort = 65536

  def availablePort(): Int =
    LazyList.range(minPort, maxPort).find { port =>
      // Consider an available port to be exclusively acquired if a lock file was newly atomically created
      val lockFile = Network.lockDirectory.resolve(f"port-$port%05d.lock").toFile
      lockFile.createNewFile() && {
        lockFile.deleteOnExit()
        portAvailable(port)
      }
    }.getOrElse(throw new IllegalStateException("No available ports found"))

  private def portAvailable(port: Int): Boolean =
    Try(new ServerSocket(port)).map(_.close()).isSuccess
}

case object Network {
  private val targetDirectoryProperty = "project.target"
  private val targetDirectoryDefault = "target"

  private lazy val lockDirectory: Path = {
    val targetDir = Paths.get(Option(System.getProperty(targetDirectoryProperty)).getOrElse(targetDirectoryDefault))
    if (!Files.exists(targetDir)) {
      throw new IllegalStateException(s"Target directory does not exist: $targetDir")
    }
    val lockDir = targetDir.resolve("lock")
    Files.createDirectories(lockDir)
    lockDir
  }
}
