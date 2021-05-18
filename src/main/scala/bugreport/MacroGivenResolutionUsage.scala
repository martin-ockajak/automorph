package bugreport

object  MacroGivenResolutionUsage:

  def main(args: Array[String]): Unit =
    println(
      foo(5, A())
    )
