package automorph.specification

import automorph.specification.OpenApi
import automorph.specification.jsonschema.Schema
import automorph.specification.openapi.RpcSchema
import automorph.spi.protocol.{RpcFunction, RpcParameter}
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import test.base.BaseTest

class OpenApiTest extends BaseTest {
  private lazy val objectMapper = (new ObjectMapper() with ClassTagExtensions)
    .registerModule(DefaultScalaModule)
    .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    .configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, true)
    .setSerializationInclusion(Include.NON_ABSENT)
    .setDefaultLeniency(false)
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
  private val functionSchemas = Seq(
    function -> RpcSchema(
    Schema(
      Some(OpenApi.objectType),
      Some(OpenApi.requestTitle),
      Some(s"Test ${OpenApi.requestTitle}"),
      Some(Map(
        "function" -> Schema(Some("string"), Some("function"), Some("Invoked function name")),
        "arguments" -> Schema(
          Some(OpenApi.objectType),
          Some(function.name),
          Some(OpenApi.argumentsDescription),
          Option(Schema.parameters(function)).filter(_.nonEmpty),
          Option(Schema.requiredParameters(function)).filter(_.nonEmpty)
        )
      )),
      Some(List("function", "arguments"))
    ), Schema(
      Some(OpenApi.objectType),
      Some(OpenApi.resultTitle),
      Some(s"Test ${OpenApi.resultTitle}"),
      Some(Map("result" -> Schema.result(function))),
      Some(List("result"))
    ), Schema(
      Some(OpenApi.objectType),
      Some(OpenApi.errorTitle),
      Some(s"Test ${OpenApi.errorTitle}"),
      Some(Map(
        "error" -> Schema(
          Some("string"),
          Some("error"),
          Some("Failed function call error details"),
          Some(Map(
            "message" -> Schema(Some("string"), Some("message"), Some("Error message"))
          )),
          Some(List("message"))
        )
      )),
      Some(List("error"))
    )
  ))
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
      |  "openapi": "3.1.0",
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
    "Description" in {
      val description = OpenApi(functionSchemas)
      val descriptionJson = objectMapper.writerWithDefaultPrettyPrinter
        .writeValueAsString(objectMapper.valueToTree(description))
//      println(descriptionJson)
//      descriptionJson.should(equal(expectedJson))
    }
  }
}
