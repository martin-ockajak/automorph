package bugreport

import scala.quoted.{Expr, Quotes, Type, quotes}

inline def foo(inline x:Int, a:A): Int = ${foo('x, 'a)}

private def foo(x: Expr[Int], a:Expr[A])(using quotes: Quotes): Expr[Int] = '{$a.plus($x)}

// does NOT work if you move the next two lines inside class A
final case class Context()
given Context = Context()

class A:
  def plus(i:Int)(using Context):Int = i+1

