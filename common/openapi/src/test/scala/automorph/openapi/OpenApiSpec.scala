//package automorph.openapi
//
//import automorph.Handler
//import automorph.protocol.JsonRpcProtocol
//import automorph.system.IdentitySystem
//import automorph.system.IdentitySystem.Identity
//import test.base.BaseSpec
//import test.{SimpleApi, SimpleApiImpl}
//
//class OpenApiSpec extends BaseSpec {
//  import io.circe.syntax.EncoderOps
//
//  private val system = IdentitySystem()
//  private val simpleApiInstance: SimpleApi[Identity] = SimpleApiImpl(system)
//
//  "" - {
//    "Test" in {
//      val codec = CirceJsonCodec()
//      val protocol = JsonRpcProtocol(codec)
//      val handler = Handler[CirceJsonCodec.Node, CirceJsonCodec, Identity, Unit](codec, system, protocol)
//        .bind(simpleApiInstance)
//      val rpcFunctions = handler.bindings.values.map(_.function)
//      val specification = OpenApi.jsonRpcSpec(rpcFunctions, "Test", "0.0", Seq("http://localhost:80/api"))
//      println(specification)
//    }
//  }
//}
