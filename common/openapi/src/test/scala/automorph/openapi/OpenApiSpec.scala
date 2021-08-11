//package automorph.openapi
//
//import automorph.spi.protocol.{RpcFunction, RpcParameter}
//import test.base.BaseSpec
//
//class OpenApiSpec extends BaseSpec {
//  private val functions = Seq(
//    RpcFunction(
//      "test",
//      Seq(
//        RpcParameter("foo", "String"),
//        RpcParameter("bar", "Integer"),
//        RpcParameter("alt", "Option[Map[String, Boolean]"),
//      ),
//      "Seq[String]",
//      Some("Testing function")
//    )
//  )
//
//  "" - {
//    "Test" in {
//      val specification = OpenApi.specification(functions, "Test", "0.0", Seq("http://localhost:80/api"))
//      println(specification.json)
//    }
//  }
//}
