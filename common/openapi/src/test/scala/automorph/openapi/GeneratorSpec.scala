package automorph.openapi

import automorph.Handler
import automorph.codec.json.CirceJsonCodec
import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import test.base.BaseSpec
import test.{SimpleApi, SimpleApiImpl}

class GeneratorSpec extends BaseSpec {
  import io.circe.syntax.EncoderOps

  private val system = IdentitySystem()
  private val simpleApiInstance: SimpleApi[Identity] = SimpleApiImpl(system)

  "" - {
    "Test" in {
      val codec = CirceJsonCodec()
      val handler = Handler[CirceJsonCodec.Node, CirceJsonCodec, Identity, Unit](codec, system)
        .bind(simpleApiInstance)
      val methods = handler.methodBindings.view.mapValues(_.method).toMap
      val specification = Generator.jsonRpcSpec(methods, "Test", "0.0", Seq("http://localhost:80/api"))
      println(specification)
//      println(Generator.json(specification))
    }
  }
}
