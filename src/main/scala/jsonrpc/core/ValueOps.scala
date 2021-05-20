package jsonrpc.core

case object ValueOps:
  extension [T](value:T)
    def some: Option[T] = Some(value)

    def asLeft[R]: Either[T, R] = Left(value)

    def asRight[L]: Either[L, T] = Right(value)
