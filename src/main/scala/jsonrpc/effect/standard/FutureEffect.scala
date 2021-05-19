package jsonrpc.effect.standard

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.Effect
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future as E}
import scala.util.{Success, Try, Failure}

final case class EEffect(executionContext: ExecutionContext)
  extends Effect[E]:
  
  private given ExecutionContext = executionContext

  def pure[T](value: T): E[T] =
    E.successful(value)

  def map[T, R](value: E[T], function: T => R): E[R] =
    value.map(function)

  def either[T](value: E[T]): E[Either[Throwable, T]] =
    value.transform(value => Success(value.toEither))
