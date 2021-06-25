package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import upack.Msg

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends Codec[Msg] {
  this: UpickleMessagePackCodec[Custom] =>

  override def encode[T](value: T): Msg = macro UpickleMessagePackCodecMeta.encode[Custom, T]

  override def decode[T](node: Msg): T = macro UpickleMessagePackCodecMeta.decode[Custom, T]
}

object UpickleMessagePackCodecMeta {
  def encode[Custom: c.TypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], value: c.Expr[T]): c.Expr[Msg] = {
    import c.universe._

    val valueType = weakTypeOf[T]
    c.Expr[Msg](q"""
      val writer = implicitly[$custom.Writer[$valueType]]
      $custom.writeMsg($value)($writer)
    """)
  }

  def decode[Custom: c.TypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], node: c.Expr[Msg]): c.Expr[T] = {
    import c.universe._

    val valueType = weakTypeOf[T]
    c.Expr[Msg](q"""
      val reader = implicitly[$custom.Reader[$valueType]]
      $custom.readBinary[$valueType]($node)($reader)
    """)
  }
}
