package examples

import examples.basic.{
  ApiSchemaDiscovery, AsynchronousCall, Authentication, HttpRequestMetadata, HttpResponseMetadata, OneWayMessage,
  OptionalParameters, SynchronousCall,
}
import examples.customize.{
  ArgumentsByPosition, ClientExceptions, ClientFunctionNames, CustomDataSerialization, HttpResponseStatus,
  ServerFunctionNames, ServerProtocolErrors,
}
import examples.select.{
  ClientTransport, EffectSystem, EndpointTransport, MessageCodec, RpcProtocol, ServerTransport, WebSocketTransport,
}
import test.base.BaseTest

class ExamplesTest extends BaseTest {

  "" - {
    "Quickstart" in {
      runTest(Quickstart)
    }
    "Basic" - {
      Seq[Any](
        ApiSchemaDiscovery,
        Authentication,
        AsynchronousCall,
        HttpRequestMetadata,
        HttpResponseMetadata,
        OneWayMessage,
        OptionalParameters,
        SynchronousCall,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Customize" - {
      Seq[Any](
        ArgumentsByPosition,
        ClientExceptions,
        ClientFunctionNames,
        CustomDataSerialization,
        HttpResponseStatus,
        ServerFunctionNames,
        ServerProtocolErrors,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Select" - {
      Seq[Any](
//        AmqpTransport,
        ClientTransport,
        EffectSystem,
        EndpointTransport,
        MessageCodec,
        RpcProtocol,
        ServerTransport,
        WebSocketTransport,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
  }

  private def testName(instance: Any): String = {
    val className = instance.getClass.getSimpleName
    className.substring(0, className.length - 1)
  }

  private def runTest(instance: Any): Unit =
    synchronized {
      val mainMethod = instance.getClass.getMethod("main", classOf[Array[String]])
      mainMethod.invoke(instance, Array[String]())
      ()
    }
}
