package test.base

import java.io.File
import java.net.ServerSocket
import java.nio.file.{Files, Path, Paths}
import scala.util.{Random, Try}

trait Network {
  private val MinPort = 2000
  private val MaxPortExcluded = 65536

  private lazy val random = new Random( seed = System.nanoTime)

  def availablePort(excluded: Set[Int]): Int =
    LazyList
      .continually(randomPort)
      .filterNot(excluded.contains)
      .filter(lockfileCreatedAtomically)
      .filter {
        case port if portAvailable(port) =>
          lockfileDeleteOnJvmExit(port)
          true
        case port =>
          lockfileDelete(port)
          false
      }
      .take(100_000)
      .headOption
      .getOrElse(throw new RuntimeException(s"$Network: no available ports found"))

  private def randomPort: Int =
    random.between(MinPort, MaxPortExcluded)

  private def lockfileCreatedAtomically(port: Int): Boolean =
    lockFileFor(port).createNewFile()

  private def portAvailable(port: Int): Boolean =
    Try(new ServerSocket(port)).map(_.close()).isSuccess

  private def lockfileDeleteOnJvmExit(port: Int): Unit =
    lockFileFor(port).deleteOnExit()

  private def lockfileDelete(port: Int): Unit =
    if (!lockFileFor(port).delete()) {
      throw new RuntimeException(s"$Network: could not delete lockfile ${lockFileFor(port)}")
    }

  private def lockFileFor(port: Int): File = {
    val lockFileName = f"port-$port%05d.lock"
    Network.portLockDirectory.resolve(lockFileName).toFile
  }
}

case object Network {

  private lazy val portLockDirectory: Path = {
    val currentWorkingDir: Path = Paths.get(new File(".").getCanonicalPath)
    val target: Path = currentWorkingDir.resolve("target")
    if (!Files.exists(target)) {
      throw new RuntimeException(
        s"Current working directory is possibly not the project directory (does not contain a target directory): $currentWorkingDir"
      )
    }
    val portLockDir = target.resolve("port.lock")
    Files.createDirectories(portLockDir)
    portLockDir
  }
}
