package examples

import examples.basic.*
import examples.customize.*
import examples.select.*
import org.scalatest.freespec.AnyFreeSpecLike

class ExamplesTest extends AnyFreeSpecLike {

  "" - {
    "Quickstart" in {
      runTest(Quickstart)
    }
    "Basic" - {
      Seq(
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
      Seq(
        ArgumentsByPosition,
        ClientExceptions,
        ClientFunctionNames,
//        CustomDataSerialization,
        HttpRequestMetadata,
        ServerFunctionNames,
        ServerProtocolErrors,
      ).foreach { instance =>
        testName(instance) in {
          runTest(instance)
        }
      }
    }
    "Select" - {
      Seq(
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
    }
}