package jsonrpc.spi

/**
 * JSON-RPC protocol message structure.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param jsonrpc protocol version (must be 2.0)
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @param result succesful call result value
 * @param error failed call error details
 * @tparam Node message node representation type
 */
final case class Message[Node](
  jsonrpc: Option[String],
  id: Option[Either[BigDecimal, String]],
  method: Option[String],
  params: Option[Message.Params[Node]],
  result: Option[Node],
  error: Option[MessageError[Node]]
) derives CanEqual:

  /** Message type. */
  lazy val objectType: MessageType = error.map(_ => MessageType.Error).getOrElse {
    result.map(_ => MessageType.Result).getOrElse {
      id.map(_ => MessageType.Call).getOrElse(MessageType.Notification)
    }
  }

  /** Message properties. */
  lazy val properties: Map[String, String] =
    Map(
      "Type" -> objectType.toString
    ) ++
      id.map(value => "Id" -> value.fold(_.toString, identity)) ++
      method.map(value => "Method" -> value) ++
      params.map(value => "Arguments" -> value.fold(_.size, _.size).toString) ++
      error.toSeq.flatMap { value =>
        value.code.map(code => "ErrorCode" -> code.toString) ++ value.message.map(message => "ErrorMessage" -> message)
      }

object Message:
  /** Message identifier type. */
  type Id = Either[BigDecimal, String]

  /** Request parameters type. */
  type Params[Node] = Either[List[Node], Map[String, Node]]

/**
 * JSON-RPC protocol message error structure.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param code error code
 * @param message error description
 * @param data additional error information
 * @tparam Node message node representation type
 */
final case class MessageError[Node](
  code: Option[Int],
  message: Option[String],
  data: Option[Node]
)

/** JSON-RPC message types. */
enum MessageType:
  case Call, Notification, Result, Error
