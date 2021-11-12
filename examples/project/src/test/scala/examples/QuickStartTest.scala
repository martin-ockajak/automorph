package examples

import org.scalatest.freespec.AnyFreeSpecLike

class QuickStartTest extends AnyFreeSpecLike {
  "" - {
    "Test" in {
      QuickStart.main(Array())
    }
  }
}
