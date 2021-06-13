package jsonrpc.util

import jsonrpc.util.MethodBindings.methodSignature
import jsonrpc.util.MethodBindings.validApiMethods
import scala.language.experimental.macros
import scala.reflect.macros.blackbox
import scala.concurrent.Future

object Bindings {
  def bind[T]: Unit = macro Bindings.bindMacro[T]

  def bindMacro[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unit] = {
    import c.universe._

    val ref = Reflection[c.type](c)
    val apiType = weakTypeOf[T]
    val apiMethods = validApiMethods[c.type, T, Future[_]](ref)
    val validMethods = apiMethods.flatMap(_.toOption)
    validMethods.foreach { method =>
      println(methodSignature[c.type, T](ref)(method))
    }
    c.Expr[Unit](q"""
      ()
    """)
  }
}
