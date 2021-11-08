package automorph.description.openapi

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
