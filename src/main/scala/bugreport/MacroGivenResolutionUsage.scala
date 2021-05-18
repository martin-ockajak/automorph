package bugreport

object  MacroGivenResolutionUsage:
  def main(args: Array[String]): Unit =
    println(
      foo(A(), 5)
    )
