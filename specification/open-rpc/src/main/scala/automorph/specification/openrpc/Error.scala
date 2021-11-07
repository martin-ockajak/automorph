package automorph.specification.openrpc

import automorph.specification.jsonschema.Reference

final case class Error(
  code: Int,
  message: String,
  data: Option[String] = None,
  $ref: Option[String] = None
) extends Reference
