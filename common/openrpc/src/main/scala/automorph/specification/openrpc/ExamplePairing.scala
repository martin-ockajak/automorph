package automorph.specification.openrpc

import automorph.specification.jsonschema.Reference

case class ExamplePairing(
  name: String,
  summary: Option[String] = None,
  description: Option[String] = None,
  params: Option[List[Either[Example, Reference]]] = None,
  result: Option[Either[Example, Reference]] = None
)
