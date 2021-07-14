//package com.archilogic.jsonrpc.amqp
//
//import java.net.InetAddress
//
//import com.archilogic.jsonrpc.core.BaseJsonRpcServer
//import com.rabbitmq.client.AMQP
//import com.typesafe.scalalogging.StrictLogging
//
//import scala.concurrent.ExecutionContext
//import scala.concurrent.duration.FiniteDuration
//
///**
//  * JSON-RPC over AMQP server based on RabbitMQ client.
//  * Declares specified queues.
//  * Interprets AMQP messages as JSON-RPC requests.
//  * Publishes responses to default exchange and uses 'reply-to' request property as routing key.
//  * @param url AMQP server URL in following format: amqp[s]://[username:password@]host[:port][/virtual host]
//  * @param requestTimeout request timeout
//  * @param queueNames names of queues to consume messages from
//  * @param prefetchCount maximum number of concurrently processed requests, set to 0 for unlimited
//  * @param durable declare durable queues which will survive AMQP server restart
//  * @param exclusive declare exclusive queues accessible only to this connection
//  * @param autoDelete declare autodelete queues which are deleted once they are no longer in use
//  * @param applicationId AMQP message application identifier
//  * @param executionContext execution context
//  */
//class AmqpJsonRpcServer(url: String, requestTimeout: FiniteDuration, queueNames: Traversable[String], prefetchCount: Int = 0,
//  durable: Boolean = false, exclusive: Boolean = false, autoDelete: Boolean = false, applicationId: String = InetAddress.getLocalHost.getHostName)
//  (implicit executionContext: ExecutionContext) extends BaseJsonRpcServer()(executionContext) with StrictLogging {
//  private val (connection, virtualHost) = RabbitMqCommon.setupConnection(url, requestTimeout)
//  setupQueueConsumers()
//
//  override def close(): Unit = {
//    connection.abort(AMQP.CONNECTION_FORCED, "Terminated", requestTimeout.toMillis.toInt)
//  }
//
//  private def setupQueueConsumers(): Unit = {
//    logger.info(s"Consuming messages from queues: ${queueNames.mkString(", ")}")
//    for (queueName <- queueNames) yield {
//      // create queue and allow consuming only one message without acknowledgement
//      val channel = connection.createChannel()
//      channel.queueDeclare(queueName, durable, exclusive, autoDelete, null)
//      channel.basicQos(prefetchCount)
//
//      // consume messages from the queue
//      channel.basicConsume(queueName, false, InetAddress.getLocalHost.getHostName,
//        new QueueConsumer(channel, virtualHost, applicationId, this))
//    }
//  }
//}
