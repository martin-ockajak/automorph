package bugreport

import scala.quoted.{Expr, Quotes}
// does NOT work if you move the next two lines inside class A
final case class Context()
given context:Context = Context()

class A:
  def increment(i:Int)(using Context):Int = i+1

inline def foo(inline x:Int, inline a:A): Int = ${foo('x, 'a)}

private def foo(x: Expr[Int], a:Expr[A])(using quotes: Quotes): Expr[Int] =
  '{$a.increment($x)}
//  '{$a.increment($x)(using $a.context)}

inline def bar(inline x:Int, a:A): Int =
  a.increment(x)
//  a.increment(x)(using a.context)


