package test.system

import automorph.system.ZioSystem
import zio.{Task, Unsafe}

class ZioTest extends DeferEffectSystemTest[Task] {

  lazy val system: ZioSystem[Any] = ZioSystem.default

  def execute[T](effect: Task[T]): Either[Throwable, T] =
    Unsafe.unsafe { implicit unsafe =>
      system.runtime.unsafe.run(effect).toEither.swap.map(_.getCause).swap
    }
}
