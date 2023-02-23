package examples

import org.scalatest.freespec.AnyFreeSpecLike

class QuickstartTest extends AnyFreeSpecLike {
  "" - {
    "Example" in {
      Quickstart.main(Array())
    }
  }
}
