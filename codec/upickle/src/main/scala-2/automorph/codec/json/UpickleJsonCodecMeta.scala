package automorph.codec.json

import automorph.codec.common.UpickleCustom
import automorph.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import ujson.Value

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
trait UpickleJsonCodecMeta[Custom <: UpickleCustom] extends Codec[Value] {

  override def encode[T](value: T): Value = macro UpickleJsonCodecMeta.encode[T]

  override def decode[T](node: Value): T = macro UpickleJsonCodecMeta.decode[T]
}

object UpickleJsonCodecMeta {

  def encode[T: c.WeakTypeTag](c: Context)(value: c.Expr[T]): c.Expr[Value] = {
    import c.universe.Quasiquote

    c.Expr[Value](q"""
      ${c.prefix}.custom.writeJs($value)
    """)
  }

  def decode[T: c.WeakTypeTag](c: Context)(node: c.Expr[Value]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      ${c.prefix}.custom.read[${weakTypeOf[T]}]($node)
    """)
  }
}
