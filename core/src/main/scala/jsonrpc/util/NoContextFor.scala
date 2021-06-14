package jsonrpc.util

final case class NoContextFor[T]()

case object NoContextFor {
  type NoContext = NoContextFor[NoContextFor.type]
  implicit val noContext: NoContext = NoContextFor[NoContextFor.type]()
}
