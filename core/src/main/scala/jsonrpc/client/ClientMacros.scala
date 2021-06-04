package jsonrpc.client

import java.beans.IntrospectionException
import jsonrpc.spi.{Codec, Effect}
import jsonrpc.core.ApiReflection
import jsonrpc.handler.HandlerMacros.{generateMethodHandle, methodDescription}
import jsonrpc.handler.MethodHandle
import jsonrpc.util.{Method, Reflection}
import scala.collection.immutable.ArraySeq
import scala.quoted.{quotes, Expr, Quotes, Type}

case object ClientMacros:

  /**
   * Generate proxy instance with JSON-RPC bindings for all valid public methods of an API type.
   *
   * @param codec message format codec
   * @param effect effect system
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Outcome computation outcome effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to their JSON-RPC wrapper functions
   * @throws IllegalArgumentException if invalid public methods are found in the API type
   */
  inline def bind[Node, CodecType <: Codec[Node], Outcome[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    effect: Effect[Outcome]
  ): ApiType =
    ${ bind[Node, CodecType, Outcome, Context, ApiType]('codec, 'effect) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Outcome[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType],
    effect: Expr[Effect[Outcome]]
  )(using quotes: Quotes): Expr[ApiType] =
    import ref.quotes.reflect.{asTerm, Block, Printer, Symbol, TypeDef, TypeRepr, TypeTree}
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = ApiReflection.detectApiMethods[Outcome, Context](ref, TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      throw IntrospectionException(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    // Debug prints
//    println(validMethods.map(_.lift).map(methodDescription).mkString("\n"))

    val proxy = '{
      new Runnable:
        def run(): Unit = ()
//      class X
    }

//    val generatedProxy = TypeDef(Symbol.classSymbol("Test"))
    val generatedProxy = TypeDef.copy(Symbol.spliceOwner.tree)("Test", Block(List.empty, Expr(0).asTerm))
//    println(generatedProxy.show(using Printer.TreeCode))
    println(generatedProxy)

    println(proxy.asTerm.show(using Printer.TreeCode))
    println(proxy.asTerm)

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

  private def methodDescription(method: Method): String =
    val documentation = method.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${method.signature}\n"
