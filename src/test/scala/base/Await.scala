package base

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait Await:
  extension [T](future: Future[T]) def await: T = Await.result(future, Duration.Inf)
