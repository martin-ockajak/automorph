package automorph.specification

import automorph.spi.protocol.{RpcFunction, RpcParameter}
import test.base.BaseTest

class OpenRpcTest extends BaseTest {
  private val function = RpcFunction(
    "test",
    Seq(
      RpcParameter("foo", "String"),
      RpcParameter("bar", "Integer"),
      RpcParameter("alt", "Option[Map[String, Boolean]"),
    ),
    "Seq[String]",
    Some("Test function")
  )
  private val expectedJson =
   """|{
      |  "paths": {
      |    "/test": {
      |      "post": {
      |        "requestBody": {
      |          "content": {
      |            "application/json": {
      |              "schema": {
      |                "description": "Test Request",
      |                "properties": {
      |                  "function": {
      |                    "description": "Invoked function name",
      |                    "title": "function",
      |                    "type": "string"
      |                  },
      |                  "arguments": {
      |                    "description": "Function argument values by name",
      |                    "properties": {
      |                      "foo": {
      |                        "title": "foo",
      |                        "type": "String"
      |                      },
      |                      "bar": {
      |                        "title": "bar",
      |                        "type": "Integer"
      |                      },
      |                      "alt": {
      |                        "title": "alt",
      |                        "type": "Option[Map[String, Boolean]"
      |                      }
      |                    },
      |                    "title": "test",
      |                    "type": "object"
      |                  }
      |                },
      |                "title": "Request",
      |                "type": "object",
      |                "required": [
      |                  "function",
      |                  "arguments"
      |                ]
      |              }
      |            }
      |          },
      |          "required": "true"
      |        },
      |        "responses": {
      |          "default": {
      |            "description": "Failed function call error details",
      |            "content": {
      |              "application/json": {
      |                "schema": {
      |                  "description": "Test Error",
      |                  "properties": {
      |                    "error": {
      |                      "description": "Failed function call error details",
      |                      "properties": {
      |                        "message": {
      |                          "description": "Error message",
      |                          "title": "message",
      |                          "type": "string"
      |                        }
      |                      },
      |                      "title": "error",
      |                      "type": "string",
      |                      "required": [
      |                        "message"
      |                      ]
      |                    }
      |                  },
      |                  "title": "Error",
      |                  "type": "object",
      |                  "required": [
      |                    "error"
      |                  ]
      |                }
      |              }
      |            }
      |          },
      |          "200": {
      |            "description": "Succesful function call result value",
      |            "content": {
      |              "application/json": {
      |                "schema": {
      |                  "description": "Test Result",
      |                  "properties": {
      |                    "result": {
      |                      "title": "result",
      |                      "type": "Seq[String]"
      |                    }
      |                  },
      |                  "title": "Result",
      |                  "type": "object",
      |                  "required": [
      |                    "result"
      |                  ]
      |                }
      |              }
      |            }
      |          }
      |        }
      |      },
      |      "description": "Test function"
      |    }
      |  },
      |  "openrpc": "1.2.1",
      |  "info": {
      |    "version": "0.0",
      |    "title": "Test"
      |  },
      |  "servers": [
      |    {
      |      "url": "http://localhost:7000/api"
      |    }
      |  ]
      |}""".stripMargin

  "" - {
    "Specification" in {
//      val specification = OpenRpc.specification(functionSchemas, "Test", "0.0", Seq("http://localhost:7000/api"))
//      val specificationJson = Json.serialize(specification)
//      println(specificationJson)
//      specificationJson.should(equal(expectedJson))
    }
  }
}
