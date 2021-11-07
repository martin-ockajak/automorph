package automorph.specification.openrpc

import automorph.specification.jsonschema.Reference

case class Method(
  name: String,
  tags: Option[List[Either[Tag, Reference]]] = None,
  summary: Option[String] = None,
  description: Option[String] = None,
  externalDocs: Option[ExternalDocumentation] = None,
  params: List[Either[ContentDescriptor, Reference]],
  result: Either[ContentDescriptor, Reference],
  deprecated: Option[Boolean] = None,
  servers: Option[List[Server]] = None,
  errors: Option[List[Either[Error, Reference]]] = None,
  links: Option[List[Either[Link, Reference]]] = None,
  paramStructure: Option[String] = None,
  examples: Option[List[ExamplePairing]] = None
)
