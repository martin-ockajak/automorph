package jsonrpc.codec

import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type, quotes}

object CodecMacros:

  inline def decode[Node, CodecType <: Codec[Node], T]( node: Node, codec: CodecType ): T =
    ${decode('node, 'codec)}

  private def decode[Node: Type, CodecType <: Codec[Node] : Type, T: Type](
    node: Expr[Node],
    codec: Expr[CodecType]
  )(using quotes: Quotes): Expr[T] =
    import ref.quotes.reflect.{TypeTree, asTerm}

    val ref = Reflection(quotes)
    ref.callTerm(codec.asTerm, "decode", List(TypeTree.of[T]), List(List(node.asTerm))).asExprOf[T]
