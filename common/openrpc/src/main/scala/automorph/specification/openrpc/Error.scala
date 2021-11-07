package automorph.specification.openrpc

final case class Error(
  code: Int,
  message: String,
  date: Option[String]
)
