package automorph.client.meta

import automorph.Contextual
import automorph.client.ClientBinding
import automorph.log.MacroLogger
import automorph.reflection.{MethodReflection, ClassReflection}
import automorph.spi.MessageCodec
import automorph.spi.protocol.RpcFunction
import scala.annotation.nowarn
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** RPC client layer bindings code generation. */
object ClientGenerator {

  /**
   * Generates client bindings for all valid public methods of an API type.
   *
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context message context type
   * @tparam Api API type
   * @return mapping of API method names to client function bindings
   */
  def bindings[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    codec: Codec
  ): Seq[ClientBinding[Node, Context]] = macro bindingsMacro[Node, Codec, Effect, Context, Api]

  def bindingsMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(codec: c.Expr[Codec])(implicit
    effectType: c.WeakTypeTag[Effect[?]]
  ): c.Expr[Seq[ClientBinding[Node, Context]]] = {
    import c.universe.Quasiquote
    val ref = ClassReflection[c.type](c)

    // Detect and validate public methods in the API type
    val apiMethods = MethodReflection.apiMethods[c.type, Api, Effect[?]](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match {
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors =>
        ref.c.abort(
          ref.c.enclosingPosition,
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )
    }

    // Generate bound API method bindings
    val clientBindings = validMethods.map { method =>
      generateBinding[c.type, Node, Codec, Effect, Context, Api](ref)(method, codec)
    }
    c.Expr[Seq[ClientBinding[Node, Context]]](q"""
      Seq(..$clientBindings)
    """)
  }

  @nowarn("msg=used")
  private def generateBinding[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: ClassReflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[Codec]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[?]]): ref.c.Expr[ClientBinding[Node, Context]] = {
    import ref.c.universe.{Liftable, Quasiquote, weakTypeOf}

    val nodeType = weakTypeOf[Node]
    val contextType = weakTypeOf[Context]
    val encodeArguments = generateArgumentEncoders[C, Node, Codec, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[C, Node, Codec, Effect, Context](ref)(method, codec)
    logBoundMethod[C, Api](ref)(method, encodeArguments, decodeResult)
    implicit val functionLiftable: Liftable[RpcFunction] = MethodReflection.functionLiftable(ref)
    ref.c.Expr[ClientBinding[Node, Context]](q"""
      automorph.client.ClientBinding[$nodeType, $contextType](
        ${method.lift.rpcFunction},
        $encodeArguments,
        $decodeResult,
        ${MethodReflection.acceptsContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateArgumentEncoders[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Context: ref.c.WeakTypeTag
  ](ref: ClassReflection[C])(method: ref.RefMethod, codec: ref.c.Expr[Codec]): ref.c.Expr[Map[String, Any => Node]] = {
    import ref.c.universe.{Quasiquote, weakTypeOf}
    (weakTypeOf[Node], weakTypeOf[Codec])

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create a map of method parameter names to functions encoding method argument value into a node
    //   Map(
    //     parameterNName -> (
    //       (argument: Any) => codec.encode[ParameterNType](argument.asInstanceOf[ParameterNType])
    //     )
    //     ...
    //   ): Map[String, Any => Node]
    val argumentEncoders = method.parameters.toList.zip(parameterListOffsets).flatMap { case (parameters, offset) =>
      parameters.toList.zipWithIndex.flatMap { case (parameter, index) =>
        Option.when((offset + index) != lastArgumentIndex || !MethodReflection.acceptsContext[C, Context](ref)(method)) {
          q"""
            ${parameter.name} -> (
              (argument: Any) => $codec.encode[${parameter.dataType}](argument.asInstanceOf[${parameter.dataType}])
            )
          """
        }
      }
    }
    ref.c.Expr[Map[String, Any => Node]](q"Map(..$argumentEncoders)")
  }

  private def generateDecodeResult[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag
  ](ref: ClassReflection[C])(method: ref.RefMethod, codec: ref.c.Expr[Codec])(implicit
    effectType: ref.c.WeakTypeTag[Effect[?]]
  ): ref.c.Expr[(Node, Context) => Any] = {
    import ref.c.universe.{Quasiquote, weakTypeOf}
    (weakTypeOf[Node], weakTypeOf[Codec])

    // Create a result decoding function
    //   (resultNode: Node, responseContext: Context) => codec.decode[ResultType](resultNode)
    //     OR
    //   (resultNode: Node, responseContext: Context) => Contextual(
    //     codec.decode[ContextualResultType](resultNode),
    //     responseContext
    //   )
    val nodeType = weakTypeOf[Node]
    val contextType = weakTypeOf[Context]
    val resultType = MethodReflection.unwrapType[C, Effect[?]](ref.c)(method.resultType).dealias
    MethodReflection.contextualResult[C, Context, Contextual[?, ?]](ref.c)(resultType).map { contextualResultType =>
      ref.c.Expr[(Node, Context) => Any](q"""
        (resultNode: $nodeType, responseContext: $contextType) => Contextual(
          $codec.decode[$contextualResultType](resultNode),
          responseContext
        )
      """)
    }.getOrElse {
      ref.c.Expr[(Node, Context) => Any](q"""
        (resultNode: $nodeType, _: $contextType) => $codec.decode[$resultType](resultNode)
      """)
    }
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: ClassReflection[C])(
    method: ref.RefMethod,
    encodeArguments: ref.c.Expr[Any],
    decodeResult: ref.c.Expr[Any]
  ): Unit = MacroLogger.debug(
    s"""${MethodReflection.signature[C, Api](ref)(method)} =
      |  ${ref.c.universe.showCode(encodeArguments.tree)}
      |  ${ref.c.universe.showCode(decodeResult.tree)}
      |""".stripMargin
  )
}
