package jsonrpc.util

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Bindings {
  def bind[T]: Unit = macro Bindings.bindMacro[T]

  def bindMacro[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unit] = {
    import c.universe._

    val ref = Reflection[c.type](c)
    val apiType = weakTypeOf[T]
    ref.methods(apiType)
    c.Expr[Unit](q"""
      ()
    """)
  }
}
