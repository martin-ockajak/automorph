package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import upack.Msg

private[jsonrpc] object UpickleMessagePackCodecMacros {

  def encode[Custom <: UpickleCustom, T](custom: Custom, value: T): Msg = macro encodeExpr[Custom, T]

  def encodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], value: c.Expr[T]): c.Expr[Msg] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Msg](q"""
      val writer = implicitly[$custom.Writer[$valueType]]
      $custom.writeMsg($value)(writer)
    """)
  }

  def decode[Custom <: UpickleCustom, T](custom: Custom, node: Msg): T = macro decodeExpr[Custom, T]

  def decodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(
    custom: c.Expr[Custom],
    node: c.Expr[Msg]
  ): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      val reader = implicitly[$custom.Reader[$valueType]]
      $custom.readBinary[$valueType]($node)(reader)
    """)
  }
}
