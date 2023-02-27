package examples

import examples.basic.{
  ApiSchemaDiscovery, AsynchronousCall, Authentication, DynamicPayload, HttpRequestMetadata, HttpResponseMetadata,
  OneWayMessage, OptionalParameters, SynchronousCall,
}
import examples.customize.{
  ClientExceptions, ClientFunctionNames, CustomDataSerialization, HttpResponseStatus, PositionalArguments, ServerErrors,
  ServerFunctionNames,
}
import examples.select.{
  AmqpTransport, ClientTransport, EffectSystem, EndpointTransport, MessageCodec, RpcProtocol, ServerTransport,
  WebSocketTransport,
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
        DynamicPayload,
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
        ClientExceptions,
        ClientFunctionNames,
        CustomDataSerialization,
        HttpResponseStatus,
        PositionalArguments,
        ServerFunctionNames,
        ServerErrors,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Select" - {
      Seq[Any](
        AmqpTransport,
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
