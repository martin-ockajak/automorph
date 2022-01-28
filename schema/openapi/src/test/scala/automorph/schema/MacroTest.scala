package automorph.schema

import test.base.BaseTest

class MacroTest extends BaseTest {
  "" - {
    "Macro" in {
      val api = Api()
      val invoke = Macro.invoke(api)
      println(invoke(List("1", "2", "3")))
    }
  }
}

case class Api():
  def method(p0: String, p1: String, p2: String): Any =
    List(p0, p1, p2).mkString(", ")
