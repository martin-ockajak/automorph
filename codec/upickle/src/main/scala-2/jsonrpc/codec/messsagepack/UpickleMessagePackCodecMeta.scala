package jsonrpc.codec.messagepack

import jsonrpc.codec.common.UpickleCustom
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import upack.Msg

/**
 * UPickle MessagePack codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
private[jsonrpc] trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends Codec[Msg] {
  this: UpickleMessagePackCodec[Custom] =>

  override def encode[T](value: T): Msg = macro UpickleMessagePackCodecMeta.encodeExpr[T]

  override def decode[T](node: Msg): T = macro UpickleMessagePackCodecMeta.decodeExpr[T]
}

private[jsonrpc] object UpickleMessagePackCodecMeta {

  def encodeExpr[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Msg] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Msg](q"""
      val custom = ${c.prefix}.custom
      import custom._
      val writer = implicitly[custom.Writer[$valueType]]
      custom.writeMsg($value)(writer)
    """)
  }

  def decodeExpr[T: c.WeakTypeTag](c: Context)(node: c.Expr[Msg]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      val custom = ${c.prefix}.custom
      import custom._
      val reader = implicitly[custom.Reader[$valueType]]
      custom.readBinary[$valueType]($node)(reader)
    """)
  }
}
