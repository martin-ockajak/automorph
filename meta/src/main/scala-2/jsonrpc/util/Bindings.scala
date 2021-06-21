package jsonrpc.util

import jsonrpc.protocol.MethodBindings.{methodSignature, validApiMethods}
import scala.concurrent.Future
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Bindings {

  def bind[T]: Unit = macro Bindings.bindMacro[T]

  def bindMacro[T: c.WeakTypeTag](c: blackbox.Context): c.Expr[Unit] = {
    import c.universe._

    val ref = Reflection[c.type](c)
    val apiMethods = validApiMethods[c.type, T, Future[_]](ref)
    val validMethods = apiMethods.flatMap(_.toOption)
    validMethods.foreach { method =>
      println(methodSignature[c.type, T](ref)(method))
      println(method.resultType.getClass.getName)
      method.resultType match {
        case typeRef: TypeRef => println(typeRef.dealias.finalResultType.resultType)
      }
    }
    c.Expr[Unit](q"""
      ()
    """)
  }
}
