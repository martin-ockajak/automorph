//package com.archilogic.jsonrpc.amqp
//
//import java.net.InetAddress
//
//import com.archilogic.jsonrpc.core.BaseJsonRpcServer
//import com.rabbitmq.client.AMQP.BasicProperties
//import com.rabbitmq.client.{Channel, Consumer, Envelope, ShutdownSignalException}
//
//import scala.concurrent.ExecutionContext
//import scala.util.Try
//
///**
//  * JSON-RPC over AMQP queue consumer for RabbitMQ client.
//  * Interprets AMQP messages as JSON-RPC requests.
//  * Publishes responses to default exchange and uses 'reply-to' request property as routing key.
//  * @param channel AMQP channel
//  * @param virtualHost AMQP server virtual host
//  * @param applicationId AMQP message application identifier
//  * @param executionContext execution context
//  */
//class JsonRpcQueueConsumer(channel: Channel, virtualHost: String, applicationId: String = InetAddress.getLocalHost.getHostName)
//  (implicit executionContext: ExecutionContext) extends BaseJsonRpcServer()(executionContext) with Consumer {
//  private val queueConsumer = new QueueConsumer(channel, applicationId, virtualHost, this)
//
//  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
//    queueConsumer.handleDelivery(consumerTag, envelope, properties, body)
//  }
//
//  override def close(): Unit = {
//    Try(channel.close())
//  }
//
//  override def handleCancel(consumerTag: String): Unit = queueConsumer.handleCancel(consumerTag)
//
//  override def handleRecoverOk(consumerTag: String): Unit = queueConsumer.handleRecoverOk(consumerTag)
//
//  override def handleCancelOk(consumerTag: String): Unit = queueConsumer.handleCancelOk(consumerTag)
//
//  override def handleShutdownSignal(consumerTag: String, sig: ShutdownSignalException): Unit = queueConsumer.handleShutdownSignal(consumerTag, sig)
//
//  override def handleConsumeOk(consumerTag: String): Unit = queueConsumer.handleConsumeOk(consumerTag)
//}
//
