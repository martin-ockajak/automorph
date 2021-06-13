package jsonrpc.util

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
      println(methodDescription(ref)(apiType, method))
    }
    c.Expr[Unit](q"""
      ()
    """)
  }

  /**
   * Create API method description.
   *
   * @param ref reflection
   * @param method method
   * @tparam ApiType API type
   * @return method description
   */
  def methodDescription[Context <: blackbox.Context](ref: Reflection[Context])(apiType: ref.c.Type, method: ref.RefMethod): String = {
    val documentation = method.lift.documentation.map(_ + "\n").getOrElse("")
    s"$documentation${apiType.typeSymbol.fullName}.${method.lift.signature}"
  }
}
