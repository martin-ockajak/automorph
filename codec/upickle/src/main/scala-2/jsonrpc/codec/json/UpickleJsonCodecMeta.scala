package jsonrpc.codec.json

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import ujson.Value

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleJsonCodecMeta[Custom <: UpickleCustom] extends Codec[Value] {
  this: UpickleJsonCodec[Custom] =>

  override def encode[T](value: T): Value = UpickleJsonCodecMeta.encode(custom, value)

  override def decode[T](node: Value): T = UpickleJsonCodecMeta.decode(custom, node)
}

object UpickleJsonCodecMeta {

  def encode[Custom <: UpickleCustom, T](custom: Custom, value: T): Value = macro encodeExpr[Custom, T]

  def encodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(
    custom: c.Expr[Custom],
    value: c.Expr[T]
  ): c.Expr[Value] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Value](q"""
      val writer = implicitly[$custom.Writer[$valueType]]
      $custom.writeJs($value)(writer)
    """)
  }

  def decode[Custom <: UpickleCustom, T](custom: Custom, node: Value): T = macro decodeExpr[Custom, T]

  def decodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], node: c.Expr[Value]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      val reader = implicitly[$custom.Reader[$valueType]]
      $custom.read[$valueType]($node)(reader)
    """)
  }
}
