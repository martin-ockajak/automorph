package automorph.client

import automorph.Client
import automorph.spi.MessageCodec
import automorph.util.CannotEqual

/**
 * Remote function invocation.
 *
 * @param name function name.
 * @param arguments argument names and values
 * @param argumentNodes encodec argument values
 * @param client RPC client
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class RemoteFunction[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  name: String,
  arguments: Seq[(String, Any)],
  private val argumentNodes: Seq[Node],
  private val client: Client[Node, Codec, Effect, Context]
) extends CannotEqual:
  val codec = client.protocol.codec

  /** Proxy type. */
  type Type = RemoteFunction[Node, Codec, Effect, Context]

  /**
   * Creates a copy of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Type parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args(): Type = copy(
    arguments = Seq.empty,
    argumentNodes = Seq.empty
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
    arguments = Seq(p1),
    argumentNodes = Seq(
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
    arguments = Seq(p1, p2),
    argumentNodes = Seq(
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
    arguments = Seq(p1, p2, p3),
    argumentNodes = Seq(
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
      arguments = Seq(p1, p2, p3, p4),
      argumentNodes = Seq(
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
      arguments = Seq(p1, p2, p3, p4, p5),
      argumentNodes = Seq(
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
      arguments = Seq(p1, p2, p3, p4, p5, p6),
      argumentNodes = Seq(
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
      arguments = Seq(p1, p2, p3, p4, p5, p6, p7),
      argumentNodes = Seq(
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
    client.call(name, arguments.map(_._1), argumentNodes, codec.decode[R](_), Some(context))

  /**
   * Sends a remote function notification request disregarding the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @param context request context
   * @return nothing
   */
  def tell(using context: Context): Effect[Unit] =
    client.notify(name, Some(arguments.map(_._1)), argumentNodes, Some(context))

  override def toString: String =
    s"${this.getClass.getName}(Method: $name, Arguments: $arguments)"
