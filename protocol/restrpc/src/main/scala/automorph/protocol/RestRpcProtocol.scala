package automorph.protocol

import automorph.protocol.RestRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.protocol.restrpc.{ErrorMapping, RestRpcCore}
import automorph.spi.{MessageCodec, RpcProtocol}

/**
 * REST-RPC protocol implementation.
 *
 * @constructor Creates a REST-RPC 2.0 protocol implementation.
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a REST-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding REST-RPC error
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class RestRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Option[Int]) => Throwable = defaultErrorToException,
  exceptionToError: Throwable => Option[Int] = defaultExceptionToError
) extends RestRpcCore[Node, Codec] with RpcProtocol[Node]

case object RestRpcProtocol extends ErrorMapping
