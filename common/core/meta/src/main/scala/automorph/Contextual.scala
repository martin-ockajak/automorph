package automorph

/**
 * API function return type containing both the actual result and response context.
 *
 * @param result
 *   API function result
 * @param context
 *   response context
 * @tparam Result
 *   API function result type
 * @tparam Context
 *   response context type
 */
final case class Contextual[Result, Context](result: Result, context: Context)
