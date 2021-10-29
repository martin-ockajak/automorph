package automorph.client

import automorph.spi.MessageCodec

/**
 * Remote function notification.
 *
 * @constructor Creates a new remote function notification with specified RPC function name.
 * @param functionName RPC function name.
 * @param codec message codec plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
final case class RemoteNotify[Node, Codec <: MessageCodec[Node], Effect[_], Context] private (
  functionName: String,
  codec: Codec,
  private val performNotify: (String, Seq[String], Seq[Node], Option[Context]) => Effect[Unit]
) extends RemoteInvoke[Node, Codec, Effect, Context, Unit] {

  override def invoke(arguments: Seq[(String, Any)], argumentNodes: Seq[Node], requestContext: Context): Effect[Unit] =
    performNotify(functionName, arguments.map(_._1), argumentNodes, Some(requestContext))
}

object RemoteNotify {

  /**
   * Creates a new remote function notification with specified RPC function name.
   *
   * @param functionName RPC function name
   * @param codec message codec plugin
   * @param peformNotify performs an RPC notification using specified arguments
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context message context type
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context](
    functionName: String,
    codec: Codec,
    performNotify: (String, Seq[String], Seq[Node], Option[Context]) => Effect[Unit]
  ): RemoteNotify[Node, Codec, Effect, Context] =
    new RemoteNotify(functionName, codec, performNotify)
}
