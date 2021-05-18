package bugreport

import scala.quoted.{Expr, Quotes}

/**** uncomment for macro compilation error

class A:
  given context:Context = Context()
  final case class Context()
  def increment(i:Int)(using Context):Int = i+1

// regular code (no macro)
inline def foo(a:A, i:Int): Int =
  // works
  a.increment(i)

  // works
  a.increment(i)(using a.context)


inline def bar(a:A, i:Int): Int = ${bar('a, 'i)}

// macro
private def bar(a:Expr[A], i: Expr[Int])(using quotes: Quotes): Expr[Int] =
  '{
    $a.increment($i)
    // no implicit argument of type Nothing was found for parameter x$2 of method increment in class A

    $a.increment($i)(using $a.context)
    // Found: (bugreport.A#context : bugreport.A#Context)
    // Required: Nothing
  }


**/