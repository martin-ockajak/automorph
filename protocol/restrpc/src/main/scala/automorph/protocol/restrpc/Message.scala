package automorph.protocol.restrpc

/**
 * REST-RPC protocol message structure.
 *
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @param result succesful method call result value
 * @param error failed method call error details
 * @tparam Node message node type
 */
final case class Message[Node](
  method: Option[String],
  params: Option[Message.Params[Node]],
  result: Option[Node],
  error: Option[MessageError[Node]]
) {

  /** Message type. */
  lazy val messageType: MessageType = error.map(_ => MessageType.Error).getOrElse {
    result.map(_ => MessageType.Result).getOrElse {
      MessageType.Call
    }
  }

  /** Message properties. */
  lazy val properties: Map[String, String] =
    Map(
      "Type" -> messageType.toString
    ) ++
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

  /** Supported REST-RPC protocol version. */
  val version = "2.0"
}

/**
 * REST-RPC protocol message error structure.
 *
 * @see [[https://www.jsonrpc.org/specification REST-RPC protocol specification]]
 * @param message error description
 * @param code error code
 * @param details additional error information
 * @tparam Node message node type
 */
final case class MessageError[Node](
  message: Option[String],
  code: Option[Int],
  details: Option[Node]
)

/**
 * REST-RPC message type.
 */
sealed abstract class MessageType {
  /**
   * Message type name.
   *
   * @return message type name
   */
  def name: String = toString
}

object MessageType {

  /** REST-RPC method call request. */
  case object Call extends MessageType
  /** REST-RPC result response. */
  case object Result extends MessageType
  /** REST-RPC error response. */
  case object Error extends MessageType
}
