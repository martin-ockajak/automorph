package automorph.client

import automorph.spi.MessageCodec
import automorph.util.CannotEqual

final case class NamedProxy[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  functionName: String,
  private val core: ClientCore[Node, Codec, Effect, Context],
  private val codec: Codec,
  private val argumentValues: Seq[(String, Any)],
  private val encodedArguments: Seq[Node]
) extends CannotEqual:

  /** Proxy type. */
  type Type = NamedProxy[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this function proxy without argument names passing function arguments by position.
   *
   * @return function proxy without argument names passing function arguments by position
   */
  def positional: PositionalProxy[Node, Codec, Effect, Context] = PositionalProxy(
    functionName,
    core,
    codec,
    argumentValues.map(_._2),
    encodedArguments
  )

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args(): Type = copy(
    argumentValues = Seq.empty,
    encodedArguments = Seq.empty
  )

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1](p1: (String, T1)): Type = copy(
    argumentValues = Seq(p1),
    encodedArguments = Seq(
      codec.encode(p1._2)
    )
  )

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2](p1: (String, T1), p2: (String, T2)): Type = copy(
    argumentValues = Seq(p1, p2),
    encodedArguments = Seq(
      codec.encode(p1._2),
      codec.encode(p2._2)
    )
  )

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3](p1: (String, T1), p2: (String, T2), p3: (String, T3)): Type = copy(
    argumentValues = Seq(p1, p2, p3),
    encodedArguments = Seq(
      codec.encode(p1._2),
      codec.encode(p2._2),
      codec.encode(p3._2)
    )
  )

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3, T4](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4)
  ): Type =
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
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3, T4, T5](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5)
  ): Type =
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
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3, T4, T5, T6](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6)
  ): Type =
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
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3, T4, T5, T6, T7](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5),
    p6: (String, T6),
    p7: (String, T7)
  ): Type =
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
   * Sends a remote function call request with specified result type extracted from the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  inline def call[R](using context: Context): Effect[R] =
    core.call(functionName, Some(argumentValues.map(_._1)), encodedArguments, codec.decode[R](_), Some(context))

  /**
   * Sends a remote function notification request disregarding the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    core.notify(functionName, Some(argumentValues.map(_._1)), encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $functionName, Arguments: $argumentValues)"
