package jsonrpc.core

import java.io.IOException
import jsonrpc.core.Protocol
import jsonrpc.core.Protocol.{ErrorType, InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import jsonrpc.spi.{CallError, Message}
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
  id: Protocol.Id,
  value: Either[CallError[Node], Node]
):

  def message: Message[Node] = Message[Node](
    jsonrpc = Protocol.version.asSome,
    id = id.asSome,
    method = None,
    params = None,
    result = value.toOption,
    error = value.swap.toOption
  )

  lazy val details: Map[String, String] = Map(
    "Type" -> value.fold(_ => ResponseType.Error, _ => ResponseType.Result).toString,
    "Id" -> id.fold(_.toString, identity)
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
      Response(id, Protocol.mandatory(message.error, "error").asLeft)
    }

/** JSON-RPC response types. */
enum ResponseType:
  case Result, Error
