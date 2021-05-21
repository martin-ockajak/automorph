package jsonrpc.core

case object ValueOps:

  extension [T](value: T)
    def asSome: Option[T] = Some(value.nn)

    def asOption: Option[T] = Option(value)

    def asLeft[R]: Either[T, R] = Left(value.nn)

    def asRight[L]: Either[L, T] = Right(value.nn)

    def classNameSimple:String = value.getClass.getSimpleName
  