package automorph.description

import automorph.description.OpenRpc
import automorph.description.jsonschema.Schema
import automorph.description.openrpc.{ContentDescriptor, Info, Method}
import automorph.spi.protocol.{RpcFunction, RpcParameter}
import test.base.BaseTest

class OpenRpcTest extends BaseTest {
  private val function = RpcFunction(
    "test",
    Seq(
      RpcParameter("foo", "String"),
      RpcParameter("bar", "Integer"),
      RpcParameter("alt", "Option[Map[String, Boolean]")
    ),
    "Seq[String]",
    Some("Test function")
  )

  private val expected = OpenRpc(
    openrpc = "1.2.6",
    info = Info(
      title = "",
      version = ""
    ),
    methods = List(
      Method(
        name = "test",
        description = Some(value = "Test function"),
        params = List(
          ContentDescriptor(
            name = "foo",
            required = Some(value = true),
            schema = Schema(
              `type` = Some(value = "String"),
              title = Some(value = "foo")
            )
          ),
          ContentDescriptor(
            name = "bar",
            required = Some(value = true),
            schema = Schema(
              `type` = Some(value = "Integer"),
              title = Some(value = "bar")
            )
          ),
          ContentDescriptor(
            name = "alt",
            required = Some(value = false),
            schema = Schema(
              `type` = Some(value = "Option[Map[String, Boolean]"),
              title = Some(value = "alt")
            )
          )
        ),
        result = ContentDescriptor(
          name = "result",
          required = Some(value = true),
          schema = Schema(
            `type` = Some(value = "Seq[String]"),
            title = Some(value = "result")
          )
        ),
        paramStructure = Some(value = "either")
      )
    )
  )
  "" - {
    "Description" in {
      val description = OpenRpc(Seq(function))
      description.should(equal(expected))
    }
  }
}
