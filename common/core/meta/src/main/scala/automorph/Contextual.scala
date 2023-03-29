package automorph

/**
 * API function return type containing both the actual result and RPC response context.
 *
 * @param result
 *   API function result
 * @param context
 *   RPC response context
 * @tparam Result
 *   API function result type
 * @tparam Context
 *   RPC response context type
 */
final case class Contextual[Result, Context](result: Result, context: Context)
