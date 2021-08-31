package automorph.codec.messagepack

import automorph.codec.UpickleCustom
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import upack.Msg

/**
 * UPickle MessagePack codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
trait UpickleMessagePackMeta[Custom <: UpickleCustom] extends MessageCodec[Msg] {

  override def encode[T](value: T): Msg = macro UpickleMessagePackMeta.encode[T]

  override def decode[T](node: Msg): T = macro UpickleMessagePackMeta.decode[T]
}

case object UpickleMessagePackMeta {

  def encode[T](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Msg] = {
    import c.universe.Quasiquote

    c.Expr[Msg](q"""
      ${c.prefix}.custom.writeMsg($value)
    """)
  }

  def decode[T: c.WeakTypeTag](c: blackbox.Context)(node: c.Expr[Msg]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      ${c.prefix}.custom.readBinary[${weakTypeOf[T]}]($node)
    """)
  }
}
