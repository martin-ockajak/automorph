package jsonrpc.util

import jsonrpc.util.MethodBindings.methodSignature
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Bindings {
  def bind[T]: Unit = macro Bindings.bindMacro[T]

  def bindMacro[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unit] = {
    import c.universe._

    val ref = Reflection[c.type](c)
    val apiType = weakTypeOf[T]
    val methods = ref.methods(apiType)
    methods.foreach { method =>
      println(methodSignature[c.type, T](ref)(method))
    }
    c.Expr[Unit](q"""
      ()
    """)
  }
}
