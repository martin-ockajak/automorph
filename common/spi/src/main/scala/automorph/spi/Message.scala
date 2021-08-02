package automorph.spi

import automorph.spi.MessageType

/**
 * JSON-RPC protocol message structure.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param jsonrpc protocol version (must be 2.0)
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @param result succesful method call result value
 * @param error failed method call error details
 * @tparam Node message node type
 */
final case class Message[Node](
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Message.Params[Node]],
  result: Option[Node],
  error: Option[MessageError[Node]]
) {

  /** Message type. */
  lazy val messageType: MessageType = error.map(_ => MessageType.Error).getOrElse {
    result.map(_ => MessageType.Result).getOrElse {
      id.map(_ => MessageType.Call).getOrElse(MessageType.Notification)
    }
  }

  /** Message properties. */
  lazy val properties: Map[String, String] =
    Map(
      "Type" -> messageType.toString
    ) ++
      id.map(value => "Id" -> value.fold(_.toString, identity)) ++
      method.map(value => "Method" -> value) ++
      params.map(value => "Arguments" -> value.fold(_.size, _.size).toString) ++
      error.toSeq.flatMap { value =>
        value.code.map(code => "ErrorCode" -> code.toString) ++ value.message.map(message => "ErrorMessage" -> message)
      }
}

object Message {
  /** Message identifier type. */
  type Id = Either[BigDecimal, String]

  /** Request parameters type. */
  type Params[Node] = Either[List[Node], Map[String, Node]]

  /** Supported JSON-RPC protocol version. */
  val version = "2.0"
}

/**
 * JSON-RPC protocol message error structure.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param message error description
 * @param code error code
 * @param data additional error information
 * @tparam Node message node type
 */
final case class MessageError[Node](
  message: Option[String],
  code: Option[Int],
  data: Option[Node]
)
