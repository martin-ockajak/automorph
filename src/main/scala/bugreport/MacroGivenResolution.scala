package bugreport

import scala.quoted.{Expr, Quotes}

/**** uncomment for macro compilation error

class A:
  given context:Context = Context()
  final case class Context()
  def increment(i:Int)(using Context):Int = i+1

// regular code (no macro)
inline def bar(inline x:Int, a:A): Int =
  // works
  a.increment(x)

  // works
  a.increment(x)(using a.context)


inline def foo(x:Int, a:A): Int = ${foo('x, 'a)}

// macro
private def foo(x: Expr[Int], a:Expr[A])(using quotes: Quotes): Expr[Int] =
  '{$a.increment($x)}
  // no implicit argument of type Nothing was found for parameter x$2 of method increment in class A

  '{$a.increment($x)(using $a.context)}
  // Found: (bugreport.A#context : bugreport.A#Context) Required: Nothing


**/