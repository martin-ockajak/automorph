package bugreport

import scala.quoted.{Expr, Quotes}


class A:
  final case class Context()
  given context:Context = Context()
  def increment(i:Int)(using Context):Int = i+1

// regular code (no macro)
inline def foo(a:A, i:Int): Int =
  // works
  a.increment(i)

  // works
  a.increment(i)(using a.context)


inline def bar(inline a:A, inline i:Int): Int = ${bar('a, 'i)}

// macro
private def bar(a:Expr[A], i: Expr[Int])(using quotes: Quotes): Expr[Int] =
  '{
    val aa = $a
    aa.increment($i)
    // no implicit argument of type Nothing was found for parameter x$2 of method increment in class A

    aa.increment($i)(using aa.context)
    // Found: (bugreport.A#context : bugreport.A#Context)
    // Required: Nothing
  }

