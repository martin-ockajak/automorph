//package automorph.transport.amqp.client
//
//import java.net.InetAddress
//
//import automorph.transport.jsonrpc.core.BaseJsonRpcClient
//import com.rabbitmq.client.AMQP.BasicProperties
//import com.rabbitmq.client.{AMQP, QueueingConsumer}
//import automorph.log.Logging
//
//import scala.concurrent.{ExecutionContext, Future}
//import scala.concurrent.duration.FiniteDuration
//import scala.util.Try
//
///**
//  * JSON-RPC client over AMQP client using RabbitMQ client.
//  * @param url AMQP server URL in following format: amqp[s]://[username:password@]host[:port][/virtual_host]
//  * @param requestTimeout request timeout
//  * @param routingKey routing key for requests (typically a queue name)
//  * @param exchange exchange for routing requests, if none, default direct exchange is used
//  * @param applicationId AMQP message application identifier
//  * @param executionContext execution context
//  */
//class AmqpJsonRpcClient(url: String, requestTimeout: FiniteDuration, routingKey: String,
//  exchange: String = RabbitMqCommon.DefaultExchangeName, applicationId: String = InetAddress.getLocalHost.getHostName)
//  (implicit executionContext: ExecutionContext) extends BaseJsonRpcClient(executionContext) with StrictLogging {
//  private val DirectExchange = "direct"
//  private val DirectReplyToQueue = "amq.rabbitmq.reply-to"
//  private val (connection, virtualHost) = RabbitMqCommon.setupConnection(url, requestTimeout)
//  private val timeout = requestTimeout.toMillis.toInt
//  setupExchange()
//
//  override def sendCallRequest(requestMessage: Array[Byte]): Future[Array[Byte]] = {
//    Future {
//      val channel = connection.createChannel()
//      try {
//        val consumer = new QueueingConsumer(channel)
//        channel.basicConsume(DirectReplyToQueue, true, consumer)
//        val properties = new BasicProperties().builder().replyTo(DirectReplyToQueue).appId(applicationId).build()
//        channel.basicPublish(exchange, routingKey, true, properties, requestMessage)
//        val response = consumer.nextDelivery(timeout.toLong)
//        val envelope = response.getEnvelope
//        logger.debug(
//          s"Received response: Queue = ${envelope.getRoutingKey}, Delivery tag = ${envelope.getDeliveryTag}, Application = ${response.getProperties.getAppId}")
//        response.getBody
////        val rpcClient = new RpcClient(channel, exchange, routingKey, timeout)
////        rpcClient.checkConsumer()
////        rpcClient.primitiveCall(properties, requestMessage)
//      } finally {
//        Try(channel.close())
//      }
//    }
//  }
//
//  override def sendNotifyRequest(requestMessage: Array[Byte]): Future[Unit] = {
//    Future {
//      val channel = connection.createChannel()
//      try {
//        channel.basicPublish(exchange, routingKey, true, null, requestMessage)
//      } finally {
//        channel.close()
//      }
//    }
//  }
//
//  override def getServerId: String = {
//    "amqp://" + connection.getAddress.getHostName + ":" + connection.getPort + "/" + virtualHost + "?" + routingKey
//  }
//
//  override def close(): Unit = {
//    connection.abort(AMQP.CONNECTION_FORCED, "Terminated", timeout)
//  }
//
//  private def setupExchange(): Unit = {
//    if (exchange != RabbitMqCommon.DefaultExchangeName) {
//      // declare direct exchange
//      val channel = connection.createChannel()
//      try {
//        channel.exchangeDeclare(exchange, DirectExchange, false)
//      } finally {
//        channel.close()
//      }
//    }
//  }
//}
