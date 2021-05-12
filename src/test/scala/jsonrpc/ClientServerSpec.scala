//package jsonrpc
//
//import base.BaseSpec
//import enumeratum.values.{IntEnum, IntEnumEntry, Json4s}
//import jsonrpc.core.Protocol.{ErrorCodes, JsonRpcException}
//import jsonrpc.{JsonRpcClient, JsonRpcServer}
//import org.json4s.jackson.JsonMethods
//import org.json4s.{DefaultFormats, Extraction, Formats, JValue, StreamInput, StringInput}
//import scala.concurrent.Future
//
//trait ClientServerSpec extends BaseSpec {
//  val jsonFormats: Formats = DefaultFormats + Json4s.serializer(Enumeration)
//
//  private var client: JsonRpcClient = _
//  private var server: JsonRpcServer = _
//
//  "" - {
//    "method registration" - {
//      "should affect registered method names" in {
//        val testFunction = (a: Int) => a + 1
//        val testProcedure = (_: Int) => Future.successful(())
//        server.methodNames().contains("test") shouldBe false
//        server.registerSync("test", testFunction)
//        server.registerAsync("test", testProcedure)
//        server.methodNames().contains("test") shouldBe true
//        server.deregister("test")
//        server.methodNames().contains("test") shouldBe false
//      }
//    }
//
//    "method notification" - {
//      "procedure with 0 arguments" in {
//        remoteNotify("method4")
//      }
//      "procedure with 1 argument" in {
//        remoteNotify("method5")
//      }
//    }
//
//    "method call" - {
//      val api = new Api
//      " with 0 arguments" in {
//        call[Double](api, "0") shouldBe 1.2d
//      }
//      " with 1 argument" in {
//        call[Int](
//          api,
//          "1",
//          Record("x", boolean = true, 0, 1, Some(2), 3, 4.5f, 6.7d, Enumeration.Zero, List(8, 9), Map("foo" -> "y", "bar" -> "z"), None)
//        ) shouldBe 3
//      }
//      " with 2 arguments" in {
//        val record = Record("x", boolean = true, 0, 1, Some(2), 3, 4.5f, 6.7d, Enumeration.Zero, List(8, 9), Map("foo" -> "y", "bar" -> "z"), None)
//        call[Record](api, "2", record, "test") shouldBe record.copy(
//          string = "x - test",
//          long = 4,
//          enumeration = Enumeration.One
//        )
//      }
//      " with 3 arguments" in {
//        call[Map[String, String]](api, "3", Some(true), 8.9f, List(0, 1, 2)) shouldBe Map(
//          "boolean" -> "true",
//          "float" -> "8.9",
//          "list" -> "0, 1, 2"
//        )
//      }
//      "incorrect number of arguments" in {
//        intercept[JsonRpcException] {
//          remoteCall("3", Some("none"))
//        }.code shouldBe ErrorCodes.InvalidParams
//      }
//      "invalid method name" in {
//        intercept[JsonRpcException] {
//          remoteCall("none")
//        }.code shouldBe ErrorCodes.MethodNotFound
//      }
//    }
//  }
//
//  def createClientServer(): (JsonRpcClient, JsonRpcServer)
//
//  override protected def beforeAll(): Unit = {
//    val (newClient, newServer) = createClientServer()
//    client = newClient
//    server = newServer
//    val api = new Api
//    server.registerSync("0", api.method0 _)
//    server.registerAsync("1", api.method1 _)
//    server.registerSync("2", api.method2 _)
//    server.registerSync("3", api.method3 _)
//    server.registerAsync("method4", api.method4 _)
//    server.registerSync("method5", api.method5 _)
//  }
//
//  override protected def afterAll(): Unit = {
//    server.deregister("0")
//    server.deregister("1")
//    server.deregister("2")
//    server.deregister("3")
//    server.deregister("method4")
//    server.deregister("method5")
//    client.close()
//    server.close()
//  }
//
//  private def call[T](instance: AnyRef, method: String, arguments: Any*)(implicit manifest: Manifest[T]): T = {
//    // Call specified method locally
//    val methodInstance = instance.getClass.getMethods.find(_.getName == method).getOrElse(fail(s"Method not found: $method"))
//    val localResult = methodInstance.invoke(instance, arguments.map(_.asInstanceOf[Object]): _*)
//    val expectedResult = if (methodInstance.getReturnType == classOf[Future[_]]) {
//      await(localResult.asInstanceOf[Future[T]])
//    } else {
//      localResult.asInstanceOf[T]
//    }
//
//    // Call specified method remotely using JSON-RPC client
//    val remoteResult = await(client.call[T](method, arguments: _*))
//    remoteResult shouldBe expectedResult
//
//    // Call specified method remotely using using JSON-RPC client with arguments supplied in JSON format
//    val remoteJsonResult = remoteCall(method)
//    val expectedJsonResult = JsonMethods.parse(StringInput(JsonMethods.compact(Extraction.decompose(expectedResult)(jsonFormats))))
//    remoteJsonResult shouldBe expectedJsonResult
//    remoteResult
//  }
//
//  private def remoteCall(method: String, argumentsResource: Option[String] = None): JValue = {
//    val resource = argumentsResource.getOrElse(method)
//    val arguments = JsonMethods.parse(StreamInput(getClass.getResourceAsStream(s"/arguments/$resource.json")))
//    await(client.call[JValue](method, arguments.children: _*))
//  }
//
//  private def remoteNotify(method: String, argumentsResource: Option[String] = None): Unit = {
//    val resource = argumentsResource.getOrElse(method)
//    val arguments = JsonMethods.parse(StreamInput(getClass.getResourceAsStream(s"/arguments/$resource.json")))
//    await(client.notify(method, arguments.children: _*))
//  }
//
//  private class Api {
//
//    def 0(): Double = 1.2d
//
//    def 1(a0: Record): Future[Int] = a0.int match {
//      case Some(int) => Future.successful(int + 1)
//      case _ => Future.successful(0)
//    }
//
//    def 2(a0: Record, a1: String): Record = a0.copy(
//      string = s"${a0.string} - $a1",
//      long = a0.long + 1,
//      enumeration = Enumeration.One
//    )
//
//    def 3(a0: Option[Boolean], a1: Float, a2: List[Int]): Map[String, String] = Map(
//      "boolean" -> a0.getOrElse(false).toString,
//      "float" -> a1.toString,
//      "list" -> a2.mkString(", ")
//    )
//
//    def method4(): Future[Unit] = Future.unit
//
//    def method5(a0: String): Unit = throw new IllegalArgumentException(a0)
//  }
//}
//
//sealed abstract class Enumeration(val value: Int) extends IntEnumEntry
//
//case object Enumeration extends IntEnum[Enumeration] {
//  case object Zero extends Enumeration(0)
//  case object One extends Enumeration(1)
//
//  val values: IndexedSeq[Enumeration] = findValues
//}
//
//final case class Record(
//  string: String,
//  boolean: Boolean,
//  byte: Byte,
//  short: Short,
//  int: Option[Int],
//  long: Long,
//  float: Float,
//  double: Double,
//  enumeration: Enumeration,
//  list: List[Number],
//  map: Map[String, String],
//  none: Option[String]
//)
