package jsonrpc.codec

import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.quoted.{quotes, Expr, Quotes, Type}

object CodecMacros:

  inline def encode[Node, CodecType <: Codec[Node], T](codec: CodecType, value: T): Node =
    ${ encode('codec, 'value) }

  private def encode[Node: Type, CodecType <: Codec[Node]: Type, T: Type](codec: Expr[CodecType], value: Expr[T])(using
    quotes: Quotes
  ): Expr[Node] =
    import ref.quotes.reflect.{asTerm, TypeRepr}

    val ref = Reflection(quotes)
    ref.callTerm(codec.asTerm, "encode", List(TypeRepr.of[T]), List(List(value.asTerm))).asExprOf[Node]

  inline def decode[Node, CodecType <: Codec[Node], T](codec: CodecType, node: Node): T =
    ${ decode('codec, 'node) }

  private def decode[Node: Type, CodecType <: Codec[Node]: Type, T: Type](
    codec: Expr[CodecType],
    node: Expr[Node]
  )(using quotes: Quotes): Expr[T] =
    import ref.quotes.reflect.{asTerm, TypeRepr}

    val ref = Reflection(quotes)
    ref.callTerm(codec.asTerm, "decode", List(TypeRepr.of[T]), List(List(node.asTerm))).asExprOf[T]
