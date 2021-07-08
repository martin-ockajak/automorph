//package com.archilogic.jsonrpc.amqp.rabitmq
//
//import java.nio.charset.StandardCharsets
//
//import com.archilogic.jsonrpc.{JsonRpcMessage, JsonRpcServer}
//import com.rabbitmq.client.AMQP.BasicProperties
//import com.rabbitmq.client.{Channel, DefaultConsumer, Envelope}
//import com.typesafe.scalalogging.StrictLogging
//
//class QueueConsumer(channel: Channel, virtualHost: String, applicationId: String, jsonRpcServer: JsonRpcServer)
//  extends DefaultConsumer(channel) with StrictLogging {
//  private val clientIdPrefix = {
//    val connection = channel.getConnection
//    "amqp://" + connection.getAddress.getHostName + ":" + connection.getPort + "/" + virtualHost + "?"
//  }
//  private val MessageProperties = new BasicProperties().builder().appId(applicationId).build()
//
//  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]): Unit = {
//    logger.debug(
//      s"Received request: Queue = ${envelope.getRoutingKey}, Delivery tag = ${envelope.getDeliveryTag}, Application = ${properties.getAppId}")
//    try {
//      val clientId = clientIdPrefix + envelope.getRoutingKey + "&" + properties.getAppId
//      jsonRpcServer.handleRequest(body, sendResponse(envelope, properties.getReplyTo) _, clientId)
//    } catch {
//      case e: Exception => {
//        logger.error(s"Failed to consume message: Queue = ${envelope.getRoutingKey}, Delivery tag = ${envelope.getDeliveryTag}", e)
//        channel.basicNack(envelope.getDeliveryTag, false, true)
//      }
//    }
//  }
//
//  private def sendResponse(envelope: Envelope, replyTo: String)(response: Option[JsonRpcMessage]): Unit = {
//    response.foreach(message => {
//      if (replyTo == null) {
//        logger.warn("Failed to send response due to missing reply-to property in the request")
//      } else {
//        channel.basicPublish(RabbitMqCommon.DefaultExchangeName, replyTo, MessageProperties, message.message.getBytes(StandardCharsets.UTF_8))
//      }
//    })
//    channel.basicAck(envelope.getDeliveryTag, false)
//    logger.debug(s"Acknowledged request: Queue = ${envelope.getRoutingKey}, Delivery tag = ${envelope.getDeliveryTag}")
//  }
//}
