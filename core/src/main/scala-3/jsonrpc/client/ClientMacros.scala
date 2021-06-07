package jsonrpc.client

import jsonrpc.client.ClientMethod
import jsonrpc.core.ApiReflection.{callMethodTerm, detectApiMethods, methodDescription}
import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type, quotes}

case object ClientMacros:
  private val debugProperty = "jsonrpc.macro.debug"
  private val debugDefault = "true"
//  private val debugDefault = ""

  /**
   * Generate client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to their JSON-RPC wrapper functions
   */
  inline def bind[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType
  ): ApiType =
    ${ bind[Node, CodecType, Effect, Context, ApiType]('codec) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType]
  )(using quotes: Quotes): Expr[ApiType] =
    import ref.quotes.reflect.{Block, Printer, Symbol, TypeDef, TypeRepr, TypeTree, asTerm}
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods[Effect, Context](ref, TypeTree.of[ApiType])
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
//    println(validMethods.map(_.lift).map(method => methodDescription[ApiType](ref, method)).mkString("\n"))
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
