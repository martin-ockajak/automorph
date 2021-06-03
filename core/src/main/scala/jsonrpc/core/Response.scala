package jsonrpc.core

import jsonrpc.core.Protocol
import jsonrpc.core.Protocol.{InvalidRequestException, mandatory}
import jsonrpc.spi.Message.Id
import jsonrpc.spi.{Message, MessageError}
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome}

/**
 * JSON-RPC call response.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param id call identifier
 * @param value response value, either a result or an error
 * @tparam Node message node representation type
 */
final case class Response[Node](
  id: Id,
  value: Either[ResponseError[Node], Node]
):

  def formed: Message[Node] = Message[Node](
    jsonrpc = Protocol.version.asSome,
    id = id.asSome,
    method = None,
    params = None,
    result = value.toOption,
    error = value.swap.toOption.map(_.formed)
  )

case object Response:

  def apply[Node](message: Message[Node]): Response[Node] =
    val jsonrpc = Protocol.mandatory(message.jsonrpc, "jsonrpc")
    if jsonrpc != Protocol.version then
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    val id = Protocol.mandatory(message.id, "id")
    message.result.map { result =>
      Response(id, result.asRight)
    }.getOrElse {
      val error = Protocol.mandatory(message.error, "error")
      Response(id, ResponseError(error).asLeft)
    }

/**
 * JSON-RPC call response error.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param code error code
 * @param message error description
 * @param data additional error information
 * @tparam Node message node representation type
 */
final case class ResponseError[Node](
  code: Int,
  message: String,
  data: Option[Node]
):

  def formed: MessageError[Node] = MessageError[Node](
    code = code.asSome,
    message = message.asSome,
    data = data
  )

case object ResponseError:

  def apply[Node](error: MessageError[Node]): ResponseError[Node] =
    val code = mandatory(error.code, "code")
    val message = mandatory(error.message, "message")
    ResponseError(code, message, error.data)
