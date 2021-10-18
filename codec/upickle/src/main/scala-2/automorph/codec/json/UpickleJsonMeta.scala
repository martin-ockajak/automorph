package automorph.codec.json

import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import ujson.Value

/**
 * UPickle JSON codec plugin code generation.
 *
 * @tparam Custom customized Upickle reader and writer implicits instance type
 */
trait UpickleJsonMeta[Custom <: UpickleJsonCustom] extends MessageCodec[Value] {

  override def encode[T](value: T): Value =
    macro UpickleJsonMeta.encodeMacro[T]

  override def decode[T](node: Value): T =
    macro UpickleJsonMeta.decodeMacro[T]
}

object UpickleJsonMeta {

  def encodeMacro[T: c.WeakTypeTag](c: blackbox.Context)(value: c.Expr[T]): c.Expr[Value] = {
    import c.universe.Quasiquote

    c.Expr[Value](q"""
      ${c.prefix}.custom.writeJs($value)
    """)
  }

  def decodeMacro[T: c.WeakTypeTag](c: blackbox.Context)(node: c.Expr[Value]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    c.Expr[T](q"""
      ${c.prefix}.custom.read[${weakTypeOf[T]}]($node)
    """)
  }
}
