package automorph.spi.transport

import automorph.spi.{EffectSystem, MessageTransport}
import scala.collection.immutable.ArraySeq

/**
 * Client message transport protocol plugin.
 *
 * The underlying transport protocol must support implementation of request-response pattern.
 *
 * Used by the RPC client to send requests and receive responses to and from a remote endpoint.
 *
 * @tparam Effect effect type
 * @tparam Context message context type
 */
trait ClientMessageTransport[Effect[_], Context] extends MessageTransport {

  /** Effect system plugin. */
  val system: EffectSystem[Effect]

  /**
   * Sends a request to a remote endpoint and retrieves the response.
   *
   * An optional request context is used to supply additional information needed to send the request.
   *
   * @param requestBody request message body
   * @param requestId request correlation identifier
   * @param mediaType message media (MIME) type.
   * @param context request context
   * @return response message
   */
  def call(requestBody: ArraySeq.ofByte, requestId: String, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte]

  /**
   * Sends a request to a remote endpoint without retrieving a response.
   *
   * An optional request context is used to supply additional information needed to send the request.
   *
   * @param requestBody request message body
   * @param requestId request correlation identifier
   * @param mediaType message media (MIME) type.
   * @param context request context
   * @return nothing
   */
  def notify(requestBody: ArraySeq.ofByte, requestId: String, mediaType: String, context: Option[Context]): Effect[Unit]

  /**
   * Creates default request context.
   *
   * @return request context
   */
  def defaultContext: Context

  /**
   * Closes this client transport freeing the underlying resources.
   *
   * @return nothing
   */
  def close(): Effect[Unit]
}
