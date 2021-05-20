package jsonrpc.core

case object ValueOps:

  extension [T](value: T)
    def asSome: Option[T] = Some(value)
    def asOptionFromNullable: Option[T] = Option(value)

    def asLeft[R]: Either[T, R] = Left(value)

    def asRight[L]: Either[L, T] = Right(value)
