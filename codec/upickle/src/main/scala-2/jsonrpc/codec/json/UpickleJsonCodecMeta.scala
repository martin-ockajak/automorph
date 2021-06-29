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
trait UpickleJsonCodecMeta[Custom <: UpickleCustom] extends Codec[Value] {
  this: UpickleJsonCodec[Custom] =>

  override def encode[T](value: T): Value =
    macro UpickleJsonCodecMeta.encode[T]

  override def decode[T](node: Value): T =
    macro UpickleJsonCodecMeta.decode[T]
}

object UpickleJsonCodecMeta {

  def encode[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Value] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Value](q"""
      val custom = ${c.prefix}.custom
      import custom._
      val writer = implicitly[custom.Writer[$valueType]]
      custom.writeJs($value)(writer)
    """)
  }

  def decode[T: c.WeakTypeTag](c: Context)(node: c.Expr[Value]): c.Expr[T] = {
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
