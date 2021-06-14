package jsonrpc

import jsonrpc.util.Bindings
import org.scalatest.freespec.AnyFreeSpec
import scala.concurrent.Future

class TestSpec extends AnyFreeSpec {
  "" - {
    "Bind" in {
      Bindings.bind[TestApi[Future]]
    }
  }
}
