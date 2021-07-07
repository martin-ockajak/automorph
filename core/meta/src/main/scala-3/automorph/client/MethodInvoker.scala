package automorph.client

import automorph.spi.{Backend, Codec, Transport}
import automorph.util.CannotEqual

case class MethodInvoker[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  methodName: String,
  argumentNames: Option[Seq[String]],
  codec: ExactCodec,
  protected val backend: Backend[Effect],
  protected val transport: Transport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable,
  protected val argumentValues: Seq[Any],
  protected val encodedArguments: Seq[Node]
) extends ClientCore[Node, ExactCodec, Effect, Context] with CannotEqual:

  type MethodInvokerType = MethodInvoker[Node, ExactCodec, Effect, Context]

  override def namedArguments: Boolean = argumentNames.isDefined

  /**
   * Create a copy of this method invoker with specified method name.
   *
   * @param methodName method name
   * @return method invoker with specified method name
   */
  def method(methodName: String): MethodInvokerType = copy(methodName = methodName)

  /**
   * Create a copy of this method invoker with specified argument names passing method arguments ''by name''.
   *
   * @param argumentNames method argument names
   * @return method invoker with specified argument names passing method arguments ''by name''
   */
  def named(argumentNames: String*): MethodInvokerType = copy(argumentNames = Some(argumentNames))

  /**
   * Create a copy of this method invoker without argument names passing method arguments ''by position''.
   *
   * @return method invoker without argument names passing method arguments ''by position''
   */
  def positional: MethodInvokerType = copy(argumentNames = None)

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  def args(): MethodInvokerType = copy(
    argumentValues = Seq(),
    encodedArguments = Seq()
  )

  /**
   * Create a copy of this method invoker with specified argument values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent method argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent method parameter types.
   *
   * @return method invoker with specified method arguments
   */
  inline def args[T1](p1: T1): MethodInvokerType = copy(
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
  inline def args[T1, T2](p1: T1, p2: T2): MethodInvokerType = copy(
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
  inline def args[T1, T2, T3](p1: T1, p2: T2, p3: T3): MethodInvokerType = copy(
    argumentValues = Seq(p1, p2, p3),
    encodedArguments = Seq(
      codec.encode(p1),
      codec.encode(p2),
      codec.encode(p3)
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
  def namedArgs(): MethodInvokerType = copy(
    argumentNames = Some(Seq()),
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
  inline def namedArgs[T1](p1: (String, T1)): MethodInvokerType = copy(
    argumentNames = Some(Seq(p1._1)),
    argumentValues = Seq(p1._2),
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
  inline def namedArgs[T1, T2](p1: (String, T1), p2: (String, T2)): MethodInvokerType = copy(
    argumentNames = Some(Seq(p1._1, p2._1)),
    argumentValues = Seq(p1._2, p2._2),
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
  inline def namedArgs[T1, T2, T3](p1: (String, T1), p2: (String, T2), p3: (String, T3)): MethodInvokerType = copy(
    argumentNames = Some(Seq(p1._1, p2._1, p3._1)),
    argumentValues = Seq(p1._2, p2._2, p3._2),
    encodedArguments = Seq(
      codec.encode(p1._2),
      codec.encode(p2._2),
      codec.encode(p3._2)
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
    performCall(methodName, argumentNames, encodedArguments, codec.decode[R](_), Some(context))

  /**
   * Send a remote JSON-RPC method ''notification'' request disregarding the response.
   *
   * The specified ''request context'' is passed to the underlying message ''transport'' plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    performNotify(methodName, argumentNames, encodedArguments, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $methodName, Arguments names: $argumentNames, Arguments: $argumentValues)"
