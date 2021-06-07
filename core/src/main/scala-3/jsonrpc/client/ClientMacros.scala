package jsonrpc.client

import java.beans.IntrospectionException
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.core.ApiReflection
import jsonrpc.handler.HandlerMacros.{debugDefault, debugProperty}
import jsonrpc.handler.HandlerMethod
import jsonrpc.util.{Method, Reflection}
import scala.collection.immutable.ArraySeq
import scala.quoted.{Expr, Quotes, Type, quotes}

case object ClientMacros:
  private val debugProperty = "jsonrpc.macro.debug"
  private val debugDefault = "true"
//  private val debugDefault = ""

  /**
   * Generate proxy instance with JSON-RPC bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @param backend effect backend plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to their JSON-RPC wrapper functions
   */
  inline def bind[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    backend: Backend[Effect]
  ): ApiType =
    ${ bind[Node, CodecType, Effect, Context, ApiType]('codec, 'backend) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]]
  )(using quotes: Quotes): Expr[ApiType] =
    import ref.quotes.reflect.{asTerm, Block, Printer, Symbol, TypeDef, TypeRepr, TypeTree}
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = ApiReflection.detectApiMethods[Effect, Context](ref, TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      ref.quotes.reflect.report.throwError(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    val proxy = '{
      new Runnable:
        def run(): Unit = ()
//      class X
    }

//    val generatedProxy = TypeDef(Symbol.classSymbol("Test"))
    val generatedProxy = TypeDef.copy(Symbol.spliceOwner.tree)("Test", Block(List.empty, Expr(0).asTerm))
//    println(generatedProxy.show(using Printer.TreeCode))

    // Debug prints
    if Option(System.getenv(debugProperty)).getOrElse(debugDefault).nonEmpty then
//    println(validMethods.map(_.lift).map(method => ApiReflection.methodDescription[ApiType](ref, method)).mkString("\n"))
      println(generatedProxy)
//    println(proxy.asTerm.show(using Printer.TreeCode))
//    println(proxy.asTerm)

    // TypeDef(
    //   $anon,
    //   Template(
    //     DefDef(
    //       <init>,
    //       List(List())
    //       TypeTree[TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class <root>)),object scala),Unit)],
    //       EmptyTree
    //     ),
    //     List(
    //       Apply(Select(New(TypeTree[TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class java)),object lang),Object)]),<init>),List()),
    //       Ident(Runnable)
    //     ),
    //     ValDef(
    //       _,
    //       EmptyTree,
    //       EmptyTree
    //     ),
    //     List(
    //       DefDef(
    //         run,
    //         List(List()),
    //         Ident(Unit),
    //         Literal(Constant(()))
    //       )
    //     )
    //   )
    // ),
    // Typed(
    //   Apply(Select(New(Ident($anon)),<init>),List()),
    //   TypeTree[TypeRef(TermRef(ThisType(TypeRef(NoPrefix,module class java)),object lang),Runnable)]
    // )

    '{
      null
    }.asInstanceOf[Expr[ApiType]]
