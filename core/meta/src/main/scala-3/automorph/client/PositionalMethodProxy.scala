package automorph.client

import automorph.spi.{Backend, Codec, Transport}
import automorph.util.CannotEqual

case class PositionalMethodProxy[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  methodName: String,
  codec: ExactCodec,
  protected val backend: Backend[Effect],
  protected val transport: Transport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable,
  protected val argumentValues: Seq[Any],
  protected val encodedArguments: Seq[Node]
) extends ClientCore[Node, ExactCodec, Effect, Context] with CannotEqual:

  type PositionalMethod = PositionalMethodProxy[Node, ExactCodec, Effect, Context]
  type NamedMethod = NamedMethodProxy[Node, ExactCodec, Effect, Context]

  /**
   * Create a copy of this method invoker passing method arguments ''by name'' with specified argument names.
   *
   * @param argumentNames method argument names
   * @return method invoker passing method arguments ''by name''
   */
  def named(argumentNames: String*): NamedMethod = NamedMethodProxy(
    methodName,
    codec,
    backend,
    transport,
    errorToException,
    argumentNames.zip(argumentValues),
    Option.when(argumentNames.size == argumentValues.size)(encodedArguments).getOrElse {
      throw IllegalArgumentException(s"Supplied ${argumentNames.size} argument names instead of ${argumentValues.size} required")
    }
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args(): PositionalMethod = copy(argumentValues = Seq(), encodedArguments = Seq())

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', '/T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1](p1: T1): PositionalMethod = copy(
    argumentValues = Seq(p1),
    encodedArguments = Seq(
      codec.encode(p1)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2](p1: T1, p2: T2): PositionalMethod = copy(
    argumentValues = Seq(p1, p2),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2, T3](p1: T1, p2: T2, p3: T3): PositionalMethod = copy(
    argumentValues = Seq(p1, p2, p3),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2, T3, T4](p1: T1, p2: T2, p3: T3, p4: T4): PositionalMethod = copy(
    argumentValues = Seq(p1, p2, p3, p4),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2, T3, T4, T5](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5): PositionalMethod = copy(
    argumentValues = Seq(p1, p2, p3, p4, p5),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2, T3, T4, T5, T6](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6): PositionalMethod = copy(
    argumentValues = Seq(p1, p2, p3, p4, p5, p6),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3),
      codec.encode(p4),
      codec.encode(p5),
      codec.encode(p6)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2, T3, T4, T5, T6, T7](p1: T1, p2: T2, p3: T3, p4: T4, p5: T5, p6: T6, p7: T7): PositionalMethod =
    copy(
      argumentValues = Seq(p1, p2, p3, p4, p5, p6, p7),
      encodedArguments = Seq(
        codec.encode(p1),
        codec.encode(p2),
        codec.encode(p3),
        codec.encode(p4),
        codec.encode(p5),
        codec.encode(p6),
        codec.encode(p7)
      )
    )

  /**
   * Send a remote JSON-RPC method ''call'' request with specified result type extracted from the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  inline def call[R](using context: Context): Effect[R] =
    call(methodName, None, encodedArguments, codec.decode[R](_), Some(context))

  /**
   * Send a remote JSON-RPC method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    notify(methodName, None, encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments: $argumentValues)"
