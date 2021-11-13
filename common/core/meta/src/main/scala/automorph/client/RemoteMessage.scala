package automorph.client

import automorph.spi.MessageCodec

/**
 * Remote function message.
 *
 * @constructor Creates a new one-way remote function message with specified RPC function name.
 * @param functionName RPC function name.
 * @param codec message codec plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
final case class RemoteMessage[Node, Codec <: MessageCodec[Node], Effect[_], Context] private (
  functionName: String,
  codec: Codec,
  private val sendMessage: (String, Seq[(String, Node)], Option[Context]) => Effect[Unit]
) extends RemoteInvoke[Node, Codec, Effect, Context, Unit] {

  override def invoke(arguments: Seq[(String, Any)], argumentNodes: Seq[Node], requestContext: Context): Effect[Unit] =
    sendMessage(functionName, arguments.map(_._1).zip(argumentNodes), Some(requestContext))
}

object RemoteMessage {

  /**
   * Creates a new one-way remote function message with specified RPC function name.
   *
   * @param functionName RPC function name
   * @param codec message codec plugin
   * @param peformNotify performs an RPC message using specified arguments
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context message context type
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context](
    functionName: String,
    codec: Codec,
    sendMessage: (String, Seq[(String, Node)], Option[Context]) => Effect[Unit]
  ): RemoteMessage[Node, Codec, Effect, Context] =
    new RemoteMessage(functionName, codec, sendMessage)
}
