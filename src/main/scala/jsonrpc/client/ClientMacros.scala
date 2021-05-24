package jsonrpc.client

import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.quoted.{quotes, Expr, Quotes, Type}

object ClientMacros:

  inline def decode[Node, CodecType <: Codec[Node], T](
    node: Node,
    codec: CodecType
  ): T = ${ decode('node, 'codec) }

  private def decode[Node: Type, CodecType <: Codec[Node]: Type, T: Type](
    node: Expr[Node],
    codec: Expr[CodecType]
  )(using quotes: Quotes): Expr[T] =
    import ref.quotes.reflect.{TypeRepr, TypeTree}

    val ref = Reflection(quotes)

    val decodeCall = ref.callTerm(ref.term(codec), "decode", List(TypeTree.of[T]), List(List(ref.term(node)))).asExpr
    '{
      $decodeCall.asInstanceOf[T]
    }
