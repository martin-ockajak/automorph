package automorph.protocol.restrpc

/**
 * REST-RPC call request.
 */
private[automorph] case object Request {
  /**
   * REST-RPC call arguments.
   *
   * @tparam Node message node type
   */
  type Arguments[Node] = Map[String, Node]
}
