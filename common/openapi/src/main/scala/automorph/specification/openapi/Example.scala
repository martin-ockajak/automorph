package automorph.specification.openapi

case class Example(
  summary: Option[String] = None,
  description: Option[String] = None,
  value: Option[String],
  externalValue: Option[String] = None
)
