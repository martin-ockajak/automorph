package examples

import org.scalatest.freespec.AnyFreeSpecLike

class QuickstartTest extends AnyFreeSpecLike {
  "" - {
    "Test" in {
      Quickstart.main(Array())
    }
  }
}
