package automorph.transport.http

import automorph.log.Logger

/**
 * HTTP & WebSocket message logger.
 *
 * @param logger logger
 * @param protocol transport protocol
 */
private[automorph] final case class HttpLog(logger: Logger, protocol: Protocol) {

  def sendingRequest(requestProperties: => Map[String, String]): Unit =
    logger.trace(s"Sending $protocol request", requestProperties)

  def sentRequest(requestProperties: => Map[String, String]): Unit =
    logger.debug(s"Sent $protocol request", requestProperties)

  def failedRequest(error: Throwable, requestProperties: => Map[String, String]): Unit =
    logger.error(s"Failed to send $protocol request", error, requestProperties)

  def receivingRequest(requestProperties: => Map[String, String]): Unit =
    logger.trace(s"Receiving $protocol request", requestProperties)

  def receivedRequest(requestProperties: => Map[String, String]): Unit =
    logger.debug(s"Received $protocol request", requestProperties)

  def sendingResponse(responseProperties: => Map[String, String]): Unit =
    logger.trace(s"Sending $protocol response", responseProperties)

  def sentResponse(responseProperties: => Map[String, String]): Unit =
    logger.debug(s"Sent $protocol response", responseProperties)

  def failedResponse(error: Throwable, responseProperties: => Map[String, String]): Unit =
    logger.error(s"Failed to receive $protocol response", error, responseProperties)

  def receivingResponse(responseProperties: => Map[String, String]): Unit =
    logger.trace(s"Receiving $protocol response", responseProperties)

  def receivedResponse(responseProperties: => Map[String, String]): Unit =
    logger.debug(s"Received $protocol response", responseProperties)

  def failedProcessing(error: Throwable, requestProperties: => Map[String, String]): Unit =
    logger.error(s"Failed to process $protocol request", error, requestProperties)
}
