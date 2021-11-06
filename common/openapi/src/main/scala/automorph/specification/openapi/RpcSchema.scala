package automorph.specification.openapi

/**
 * RPC API function schema.
 *
 * @param request request schema
 * @param result result schema
 * @param error error schema
 */
private [automorph] final case class RpcSchema(
  request: Schema,
  result: Schema,
  error: Schema
)
