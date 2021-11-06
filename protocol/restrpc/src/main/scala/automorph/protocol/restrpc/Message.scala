package automorph.protocol.restrpc

import scala.collection.immutable.ListMap

/**
 * REST-RPC protocol message structure.
 *
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param result succesful method call result value
 * @param error failed method call error details
 * @tparam Node message node type
 */
final case class Message[Node](
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
  lazy val properties: Map[String, String] = ListMap(
    "Type" -> messageType.toString
  ) ++
    error.toSeq.flatMap { value =>
      value.code.map(code => "Error Code" -> code.toString) ++ value.message.map(message => "ErrorMessage" -> message)
    }
}

object Message {
  /** Request parameters type. */
  type Request[Node] = Map[String, Node]
}

/**
 * REST-RPC protocol message error structure.
 *
 * @see [[https://www.jsonrpc.org/specification REST-RPC protocol specification]]
 * @param message error message
 * @param code error code
 * @param details additional error information
 * @tparam Node message node type
 */
final case class MessageError[Node](
  message: Option[String],
  code: Option[Int],
  details: Option[Node]
)

/** REST-RPC message type. */
sealed abstract class MessageType {
  /** Message type name. */
  def name: String = toString
}

object MessageType {

  /** REST-RPC function call request. */
  case object Call extends MessageType
  /** REST-RPC result response. */
  case object Result extends MessageType
  /** REST-RPC error response. */
  case object Error extends MessageType
}
