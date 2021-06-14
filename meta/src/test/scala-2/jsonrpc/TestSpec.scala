package jsonrpc

import base.BaseSpec
import jsonrpc.util.Bindings
import scala.concurrent.Future

class TestSpec extends BaseSpec {
  "" - {
    "Bind" in {
      Bindings.bind[TestApi[Future]]
    }
  }
}
