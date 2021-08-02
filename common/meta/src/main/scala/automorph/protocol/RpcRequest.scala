package automorph.protocol

final case class RpcRequest[Node, Properties](
  method: String,
  arguments: Either[List[Node], Map[String, Node]],
  properties: Properties
)
