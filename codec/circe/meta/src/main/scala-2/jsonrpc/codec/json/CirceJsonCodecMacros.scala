package jsonrpc.codec.json

import io.circe.Json
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

private[jsonrpc] object CirceJsonCodecMacros {

  def encode[Custom <: CirceCustom, T](custom: Custom, value: T): Json = macro encodeExpr[Custom, T]

  def encodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.{weakTypeOf, Quasiquote}
    weakTypeOf[Custom]

    val valueType = weakTypeOf[T]
    c.Expr[Json](q"""
      val encoder = implicitly[$custom.CirceEncoder[$valueType]].encoder
      $value.asJson(encoder)
    """)
  }

  def decode[Custom <: CirceCustom, T](custom: Custom, node: Json): T = macro decodeExpr[Custom, T]

  def decodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}
    weakTypeOf[Custom]

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      val decoder = implicitly[$custom.CirceDecoder[$valueType]].decoder
      $node.as[$valueType](decoder).toTry.get
    """)
  }
}
