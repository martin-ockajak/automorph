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

  override def encode[T](value: T): Msg = UpickleMessagePackCodecMeta.encode(custom, value)

  override def decode[T](node: Msg): T = UpickleMessagePackCodecMeta.decode(custom, node)
}

object UpickleMessagePackCodecMeta {

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
    c.Expr[Msg](q"""
      val reader = implicitly[$custom.Reader[$valueType]]
      $custom.readBinary[$valueType]($node)(reader)
    """)
  }
}
