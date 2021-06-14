package jsonrpc.util

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object Bindings {

  def bind[T]: Unit = macro Bindings.bindMacro[T]

  def bindMacro[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unit] = {
    import c.universe._

    val ref = Reflection[c.type](c)
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
