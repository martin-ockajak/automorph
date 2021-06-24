package jsonrpc.client

import jsonrpc.client.ClientMethod
import jsonrpc.protocol.MethodBindings.{methodSignature, methodUsesContext, validApiMethods}
import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC client layer bindings code generation. */
private[jsonrpc] case object ClientBindings {

  private val debugProperty = "jsonrpc.macro.debug"
//  private val debugDefault = "true"
  private val debugDefault = ""

  /**
   * Generate client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to client method bindings
   */
  def generate[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType
  ): Map[String, ClientMethod[Node]] = macro generateExpr[Node, CodecType, Effect, Context, ApiType]

  def generateExpr[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    ApiType <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(codec: c.Expr[CodecType])(implicit
    effectType: c.WeakTypeTag[Effect[_]]
  ): c.Expr[Map[String, ClientMethod[Node]]] = {
    import c.universe.Quasiquote
    val ref = Reflection(c)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[ApiType, Effect[_]](ref)
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if (invalidMethodErrors.nonEmpty) {
      ref.c.abort(
        ref.c.enclosingPosition,
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )
    }

    // Generate bound API method bindings
    val clientMethods = validMethods.map { method =>
      generateClientMethod[Node, CodecType, Effect, Context, ApiType](c, ref)(method, codec)
    }
    c.Expr[Map[String, ClientMethod[Node]]](q"""
      Seq(..$clientMethods).toMap
    """)
  }

  private def generateClientMethod[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    ApiType: c.WeakTypeTag
  ](c: blackbox.Context, ref: Reflection)(
    method: ref.RefMethod,
    codec: c.Expr[CodecType]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[(String, ClientMethod[Node])] = {
    import c.universe._

    val liftedMethod = method.lift
    val encodeArguments = generateEncodeArguments[Node, CodecType, Context](c, ref)(method, codec)
    val decodeResult = generateDecodeResult[Node, CodecType, Effect](c, ref)(method, codec)
    val name = q"${liftedMethod.name}"
    val resultType = q"${liftedMethod.resultType}"
    val parameterNames = q"..${liftedMethod.parameters.flatMap(_.map(_.name))}"
    val parameterTypes = q"..${liftedMethod.parameters.flatMap(_.map(_.dataType))}"
    val usesContext = q"${methodUsesContext[Context](ref)(method)}"
    logBoundMethod[ApiType](c, ref)(method, encodeArguments, decodeResult)
//    '
//    {
//    $name -> ClientMethod(
//      $encodeArguments,
//      $decodeResult,
//      $name,
//      $resultType,
//      $parameterNames,
//      $parameterTypes,
//      $usesContext
//    )
//    }
    null
  }

  private def generateEncodeArguments[Node: c.WeakTypeTag, CodecType <: Codec[Node]: c.WeakTypeTag, Context: c.WeakTypeTag](
    c: blackbox.Context, ref: Reflection
  )(method: ref.RefMethod, codec: c.Expr[CodecType] ): c.Expr[Seq[Any] => Seq[Node]] = {
    import c.universe._

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

//    // Create encode arguments function
//    //   (arguments: Seq[Any]) => Seq[Node]
//    '
//    { (arguments: Seq[Any]) =>
//      $
//      {
//      // Create the method argument lists by encoding corresponding argument values into nodes
//      //   List(
//      //     codec.encode[Parameter0Type](arguments(0).asInstanceOf[Parameter0Type]),
//      //     codec.encode[Parameter1Type](arguments(1).asInstanceOf[Parameter1Type]),
//      //     ...
//      //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
//      //   ): List[Node]
//      val argumentList = method.parameters.toList.zip(parameterListOffsets).flatMap((parameters, offset) =>
//        parameters.toList.zipWithIndex.flatMap { (parameter, index) =>
//          Option.when((offset + index) != lastArgumentIndex || !methodUsesContext[Context](ref, method)) {
//            val argument = parameter.dataType.asType match
//              case
//            '[parameterType]
//            => '
//            {arguments($
//            {Expr(offset + index)}).asInstanceOf[parameterType]}
//            call(ref.q, codec.asTerm, "encode", List(parameter.dataType), List(List(argument.asTerm)))
//          }
//        }
//      ).map(_.asInstanceOf[Term].asExprOf[Node])
//
//      // Create the encoded arguments sequence construction call
//      //   Seq(encodedArguments ...): Seq[Node]
//      '
//      {Seq($
//      {Expr.ofSeq(argumentList)} *)}
//      }
//    }
    null
  }

  private def generateDecodeResult[Node: c.WeakTypeTag, CodecType <: Codec[Node]: c.WeakTypeTag, Effect[_]](
    c: blackbox.Context, ref: Reflection
  )(method: ref.RefMethod, codec: c.Expr[CodecType])(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Node => Any] = {
    import c.universe._

//    // Create decode result function
//    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
//    val resultValueType = unwrapType[Effect](ref, method.resultType)
//    '
//    { (resultNode: Node) =>
//      $
//      {
//      call(ref.q, codec.asTerm, "decode", List(resultValueType), List(List('
//        {resultNode}.asTerm))).asExprOf[Any]
//      }
//    }
    null
  }

  private def logBoundMethod[ApiType: ref.c.WeakTypeTag](c: blackbox.Context, ref: Reflection)(
    method: ref.RefMethod,
    encodeArguments: c.Expr[Any],
    decodeResult: c.Expr[Any]
  ): Unit = {
    import c.universe.showCode

    if (Option(System.getProperty(debugProperty)).getOrElse(debugDefault).nonEmpty) {
      println(
        s"""${methodSignature[ApiType](ref)(method)} =
          |  ${showCode(encodeArguments.tree)}
          |  ${showCode(decodeResult.tree)}
          |  """.stripMargin
      )
    }
  }
}
