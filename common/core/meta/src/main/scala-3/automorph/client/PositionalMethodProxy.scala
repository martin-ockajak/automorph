package automorph.client

import automorph.spi.MessageCodec
import automorph.util.CannotEqual

final case class PositionalMethodProxy[Node, Codec <: MessageCodec[Node], Effect[_], Context] (
  methodName: String,
  private val core: ClientCore[Node, Codec, Effect, Context],
  private val codec: Codec,
  private val argumentValues: Seq[Any],
  private val encodedArguments: Seq[Node]
) extends CannotEqual:

  /** Positional method proxy type. */
  type PositionalMethod = PositionalMethodProxy[Node, Codec, Effect, Context]
  /** Named method proxy type. */
  type NamedMethod = NamedMethodProxy[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this method proxy passing method arguments ''by name'' with specified argument names.
   *
   * @param argumentNames method argument names
   * @return method proxy
   */
  def named(argumentNames: String*): NamedMethod = NamedMethodProxy(
    methodName,
    core,
    codec,
    argumentNames.zip(argumentValues),
    Option.when(argumentNames.size == argumentValues.size)(encodedArguments).getOrElse {
      throw IllegalArgumentException(s"Supplied ${argumentNames.size} argument names instead of ${argumentValues.size} required")
    }
  )

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args(): PositionalMethod = copy(argumentValues = Seq.empty, encodedArguments = Seq.empty)

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', '/T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1](p1: T1): PositionalMethod = copy(
    argumentValues = Seq(p1),
    encodedArguments = Seq(
      codec.encode(p1)
    )
  )

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2](p1: T1, p2: T2): PositionalMethod = copy(
    argumentValues = Seq(p1, p2),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2)
    )
  )

  /**
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
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
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
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
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
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
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
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
   * Creates a copy of this method proxy with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
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
   * Sends a remote method ''call'' request with specified result type extracted from the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  inline def call[R](using context: Context): Effect[R] =
    core.call(methodName, None, encodedArguments, codec.decode[R](_), Some(context))

  /**
   * Sends a remote method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    core.notify(methodName, None, encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments: $argumentValues)"
