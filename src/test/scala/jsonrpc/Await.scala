package jsonrpc

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

trait Await {
  extension [T](future: Future[T])
    def await: T = Await.result[T](future, Duration.Inf)
}
