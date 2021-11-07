package automorph.specification.openrpc

case class Example(
  name: Option[String] = None,
  summary: Option[String] = None,
  description: Option[String] = None,
  value: Option[String],
  externalValue: Option[String] = None
)
