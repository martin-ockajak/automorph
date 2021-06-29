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
trait UpickleMessagePackCodecMeta[Custom <: UpickleCustom] extends Codec[Msg] {

  override def encode[T](value: T): Msg =
    macro UpickleMessagePackCodecMeta.encode[T]

  override def decode[T](node: Msg): T =
    macro UpickleMessagePackCodecMeta.decode[T]
}

object UpickleMessagePackCodecMeta {

  def encode[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Msg] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Msg](q"""
      val custom = ${c.prefix}.custom
      import custom._
      val writer = implicitly[custom.Writer[$valueType]]
      custom.writeMsg($value)(writer)
    """)
  }

  def decode[T: c.WeakTypeTag](c: Context)(node: c.Expr[Msg]): c.Expr[T] = {
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
