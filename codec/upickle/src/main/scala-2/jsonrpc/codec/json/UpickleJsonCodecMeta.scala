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

  override def encode[T](value: T): Value = macro UpickleJsonCodecMeta.encodeExpr[T]

  override def decode[T](node: Value): T = macro UpickleJsonCodecMeta.decodeExpr[T]
}

object UpickleJsonCodecMeta {

  def encodeExpr[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Value] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Value](q"""
      val custom = ${c.prefix}.custom
      import custom._
      val writer = implicitly[custom.Writer[$valueType]]
      custom.writeJs($value)(writer)
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: Context)(node: c.Expr[Value]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      val custom = ${c.prefix}.custom
      import custom._
      val reader = implicitly[custom.Reader[$valueType]]
      custom.read[$valueType]($node)(reader)
    """)
  }
}
