package automorph.client

import automorph.spi.MessageCodec
import automorph.util.CannotEqual

case class NamedMethodProxy[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  methodName: String,
  private val core: ClientCore[Node, Codec, Effect, Context],
  private val codec: Codec,
  private val argumentValues: Seq[(String, Any)],
  private val encodedArguments: Seq[Node]
) extends CannotEqual:

  /** Positional method proxy type. */
  type PositionalMethod = PositionalMethodProxy[Node, Codec, Effect, Context]
  /** Named method proxy type. */
  type NamedMethod = NamedMethodProxy[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this method proxy without argument names passing method arguments ''by position''.
   *
   * @return method proxy without argument names passing method arguments ''by position''
   */
  def positional: PositionalMethod = PositionalMethodProxy(
    methodName,
    core,
    codec,
    argumentValues.map(_._2),
    encodedArguments
  )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  def args(): NamedMethod = copy(
    argumentValues = Seq.empty,
    encodedArguments = Seq.empty
  )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1](p1: (String, T1)): NamedMethod = copy(
    argumentValues = Seq(p1),
    encodedArguments = Seq(
      codec.encode(p1._2)
    )
  )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2](p1: (String, T1), p2: (String, T2)): NamedMethod = copy(
    argumentValues = Seq(p1, p2),
    encodedArguments = Seq(
      codec.encode(p1._2),
      codec.encode(p2._2)
    )
  )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2, T3](p1: (String, T1), p2: (String, T2), p3: (String, T3)): NamedMethod = copy(
    argumentValues = Seq(p1, p2, p3),
    encodedArguments = Seq(
      codec.encode(p1._2),
      codec.encode(p2._2),
      codec.encode(p3._2)
    )
  )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2, T3, T4](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4)
  ): NamedMethod =
    copy(
      argumentValues = Seq(p1, p2, p3, p4),
      encodedArguments = Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2)
      )
    )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2, T3, T4, T5](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5)
  ): NamedMethod =
    copy(
      argumentValues = Seq(p1, p2, p3, p4, p5),
      encodedArguments = Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2),
        codec.encode(p5._2)
      )
    )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2, T3, T4, T5, T6](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6)
  ): NamedMethod =
    copy(
      argumentValues = Seq(p1, p2, p3, p4, p5, p6),
      encodedArguments = Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2),
        codec.encode(p5._2),
        codec.encode(p6._2)
      )
    )

  /**
   * Creates a copy of this method proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method proxy
   */
  inline def args[T1, T2, T3, T4, T5, T6, T7](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6),
    p7: (String, T7)
  ): NamedMethod =
    copy(
      argumentValues = Seq(p1, p2, p3, p4, p5, p6, p7),
      encodedArguments = Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2),
        codec.encode(p5._2),
        codec.encode(p6._2),
        codec.encode(p7._2)
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
    core.call(methodName, Some(argumentValues.map(_._1)), encodedArguments, codec.decode[R](_), Some(context))

  /**
   * Sends a remote method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    core.notify(methodName, Some(argumentValues.map(_._1)), encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments: $argumentValues)"
