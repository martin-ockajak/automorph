package test.base

import java.io.File
import java.net.ServerSocket
import java.nio.file.{Files, Path, Paths}
import scala.util.{Random, Try}

trait Network {

  def availablePort(excluded: Set[Int]): Int =
    LazyList
      .continually(Network.randomPort)
      .filterNot(excluded.contains)
      .take(100_000)
      .filter(lockFileWasCreatedAtomically)
      .filter {
        case port if portIsAvailable(port) =>
          deleteLockfileOnJvmExit(port)
          true
        case port =>
          deleteLockfile(port)
          false
      }
      .headOption
      .getOrElse(throw new RuntimeException(s"$Network: no available ports found"))

  private def lockFileWasCreatedAtomically(port: Int): Boolean =
    lockFileFor(port).createNewFile()

  private def portIsAvailable(port: Int): Boolean =
    Try(new ServerSocket(port)).map(_.close()).isSuccess

  private def deleteLockfileOnJvmExit(port: Int): Unit =
    lockFileFor(port).deleteOnExit()

  private def deleteLockfile(port: Int): Unit =
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

  private lazy val random = {
    val random = new Random
    random.setSeed(System.nanoTime)
    random
  }

  private def randomPort: Int =
    random.between(2000, 65536)
}
