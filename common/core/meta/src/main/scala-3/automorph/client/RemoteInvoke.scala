package automorph.client

import automorph.spi.MessageCodec
import automorph.Contextual
import automorph.util.{CannotEqual, MethodReflection}
import scala.quoted.{Expr, Quotes, Type}

/**
 * RPC function invocation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 * @tparam Result result type
 */
private[automorph] trait RemoteInvoke[Node, Codec <: MessageCodec[Node], Effect[_], Context, Result]
  extends CannotEqual:

  /** RPC function name. */
  val functionName: String

  /** Message codec plugin. */
  def codec: Codec

  /**
   * Sends a remote function invocation request with specified result type extracted from the response.
   *
   * The specified request context is passed to the underlying message transport plugin.
   *
   * @param argumentNames argument names
   * @param argumentNodes function argument nodes
   * @param requestContext request context
   * @return result value
   */
  def invoke(arguments: Seq[(String, Any)], argumentNodes: Seq[Node], requestContext: Context): Effect[Result]

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  def args()(using requestContext: Context): Effect[Result] = invoke(
    Seq.empty,
    Seq.empty,
    requestContext
  )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1](p1: (String, T1))(using requestContext: Context): Effect[Result] = invoke(
    Seq(p1),
    Seq(
      codec.encode(p1._2)
    ),
    requestContext
  )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2](p1: (String, T1), p2: (String, T2))(using requestContext: Context): Effect[Result] = invoke(
    Seq(p1, p2),
    Seq(
      codec.encode(p1._2),
      codec.encode(p2._2)
    ),
    requestContext
  )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3](p1: (String, T1), p2: (String, T2), p3: (String, T3))(using
    requestContext: Context
  ): Effect[Result] =
    invoke(
      Seq(p1, p2, p3),
      Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2)
      ),
      requestContext
    )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3, T4](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4)
  )(using requestContext: Context): Effect[Result] =
    invoke(
      Seq(p1, p2, p3, p4),
      Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2)
      ),
      requestContext
    )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
   *
   * @return function proxy
   */
  inline def args[T1, T2, T3, T4, T5](
    p1: (String, T1),
    p2: (String, T2),
    p3: (String, T3),
    p4: (String, T4),
    p5: (String, T5)
  )(using requestContext: Context): Effect[Result] =
    invoke(
      Seq(p1, p2, p3, p4, p5),
      Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2),
        codec.encode(p5._2)
      ),
      requestContext
    )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
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
  )(using requestContext: Context): Effect[Result] =
    invoke(
      Seq(p1, p2, p3, p4, p5, p6),
      Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2),
        codec.encode(p5._2),
        codec.encode(p6._2)
      ),
      requestContext
    )

  /**
   * Creates a invoke of this function proxy with specified argument names and values.
   *
   * Parameters 'p1', 'p2' ... 'pN' represent function argument values.
   * Effect[R] parameters 'T1', 'T2' ... 'TN' represent function parameter types.
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
  )(using requestContext: Context): Effect[Result] =
    invoke(
      Seq(p1, p2, p3, p4, p5, p6, p7),
      Seq(
        codec.encode(p1._2),
        codec.encode(p2._2),
        codec.encode(p3._2),
        codec.encode(p4._2),
        codec.encode(p5._2),
        codec.encode(p6._2),
        codec.encode(p7._2)
      ),
      requestContext
    )

object RemoteInvoke:

  inline def decodeResult[Node, Codec <: MessageCodec[Node], Context, R](codec: Codec): (Node, Context) => R =
    ${ decodeResultMacro[Node, Codec, Context, R]('codec) }

  private def decodeResultMacro[Node: Type, Codec <: MessageCodec[Node]: Type, Context: Type, R: Type](
    using quotes: Quotes
  )(codec: Expr[Codec]): Expr[(Node, Context) => R] =
    import quotes.reflect.{TypeRepr, asTerm}

    val resultType = TypeRepr.of[R].dealias
    MethodReflection.contextualResult[Context, Contextual](quotes)(resultType).map { contextualResultType =>
      contextualResultType.asType match
        case '[resultValueType] =>
          '{ (resultNode: Node, responseContext: Context) =>
            Contextual(
              ${
                MethodReflection.call(
                  quotes,
                  codec.asTerm,
                   "decode",
                  List(contextualResultType),
                  List(List('{ resultNode }.asTerm))
                ).asExprOf[resultValueType]
              },
              responseContext
            )
          }.asInstanceOf[Expr[(Node, Context) => R]]
    }.getOrElse {
      '{ (resultNode: Node, _: Context) =>
        ${
          MethodReflection.call(
            quotes,
            codec.asTerm,
            "decode",
            List(resultType),
            List(List('{ resultNode }.asTerm))
          ).asExprOf[R]
        }
      }
    }
