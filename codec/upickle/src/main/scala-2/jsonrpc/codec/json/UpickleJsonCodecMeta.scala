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

  override def encode[T](value: T): Json = macro UpickleJsonCodecMeta.encode[Custom, T]

  override def decode[T](node: Json): T = macro UpickleJsonCodecMeta.decode[Custom, T]
}

object UpickleJsonCodecMeta {
  def encode[Custom: c.TypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], value: c.Expr[T]): c.Expr[Value] = {
    import c.universe._

    val valueType = weakTypeOf[T]
    c.Expr[Value](q"""
      val writer = implicitly[$custom.Writer[$valueType]]
      $custom.writeJs($value)($writer)
    """)
  }

  def decode[Custom: c.TypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], node: c.Expr[Value]): c.Expr[T] = {
    import c.universe._

    val valueType = weakTypeOf[T]
    c.Expr[Value](q"""
      val reader = implicitly[$custom.Reader[$valueType]]
      $custom.read[$valueType]($node)($reader)
    """)
  }
}
