package automorph.description.openapi

import automorph.description.jsonschema.Schema

/**
 * RPC function schema.
 *
 * @param request request schema
 * @param result result schema
 * @param error error schema
 */
final case class RpcSchema(
  request: Schema,
  result: Schema,
  error: Schema
)
