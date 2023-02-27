package examples

import examples.lowlevel.{Authentication, DynamicPayload, HttpRequestMetadata, HttpResponseMetadata, PositionalArguments}
import examples.basic.{ApiSchemaDiscovery, AsynchronousCall, OneWayMessage, OptionalParameters, SynchronousCall}
import examples.customization.{
  ClientExceptions, ClientFunctionNames, CustomDataSerialization, HttpResponseStatus, ServerErrors, ServerFunctionNames,
}
import examples.integration.{
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
        AsynchronousCall,
        OneWayMessage,
        OptionalParameters,
        SynchronousCall,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Low Level" - {
      Seq[Any](
        Authentication,
        DynamicPayload,
        HttpRequestMetadata,
        HttpResponseMetadata,
        PositionalArguments,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Customization" - {
      Seq[Any](
        ClientExceptions,
        ClientFunctionNames,
        CustomDataSerialization,
        HttpResponseStatus,
        ServerFunctionNames,
        ServerErrors,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Integration" - {
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
