package automorph.client

import automorph.spi.{Backend, Codec, Transport}
import automorph.util.CannotEqual

case class NamedMethodProxy[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  methodName: String,
  codec: ExactCodec,
  protected val backend: Backend[Effect],
  protected val transport: Transport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable,
  protected val argumentValues: Seq[(String, Any)],
  protected val encodedArguments: Seq[Node]
) extends ClientCore[Node, ExactCodec, Effect, Context] with CannotEqual:

  type PositionalMethod = PositionalMethodProxy[Node, ExactCodec, Effect, Context]
  type NamedMethod = NamedMethodProxy[Node, ExactCodec, Effect, Context]

  /**
   * Create a copy of this method invoker without argument names passing method arguments ''by position''.
   *
   * @return method invoker without argument names passing method arguments ''by position''
   */
  def positional: PositionalMethod = PositionalMethodProxy(
    methodName,
    codec,
    backend,
    transport,
    errorToException,
    argumentValues.map(_._2),
    encodedArguments
  )

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args(): NamedMethod = copy(
    argumentValues = Seq(),
    encodedArguments = Seq()
  )

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1](p1: (String, T1)): NamedMethod = copy(
    argumentValues = Seq(p1),
    encodedArguments = Seq(
      codec.encode(p1._2)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1, T2](p1: (String, T1), p2: (String, T2)): NamedMethod = copy(
    argumentValues = Seq(p1, p2),
    encodedArguments = Seq(
      codec.encode(p1._2),
      codec.encode(p2._2)
    )
  )

  /**
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
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
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
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
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
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
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
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
   * Create a copy of this method invoker with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
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
   * Send a remote JSON-RPC method ''call'' request with specified result type extracted from the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @tparam R result type
   * @param context request context
   * @return result value
   */
  inline def call[R](using context: Context): Effect[R] =
    call(methodName, Some(argumentValues.map(_._1)), encodedArguments, codec.decode[R](_), Some(context))

  /**
   * Send a remote JSON-RPC method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    notify(methodName, Some(argumentValues.map(_._1)), encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments: $argumentValues)"
