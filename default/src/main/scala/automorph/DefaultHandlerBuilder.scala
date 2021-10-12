//package automorph
//
//import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
//import automorph.system.IdentitySystem.Identity
//import automorph.util.EmptyContext
//import scala.concurrent.{ExecutionContext, Future}
//
///**
// * RPC request handler builder.
// *
// * @constructor Creates a new RPC request handler builder.
// * @tparam Node message node type
// * @tparam Codec message codec plugin type
// * @tparam Effect effect type
// * @tparam Context request context type
// */
//final class DefaultHandlerBuilder[Node, Codec <: MessageCodec[Node], Effect[_], Context](
//  private val system: Option[EffectSystem[Effect]],
//  private val protocol: Option[RpcProtocol[Node, Codec]]
//) extends HandlerBuilder[Node, Codec, Effect, Context](system, protocol) {
//
//  /**
//   * Set default plugins with asynchronous effect ''system'' using `Future` as an effect type.
//   *
//   * @param executionContext execution context
//   * @return RPC request handler builder
//   */
//  def defaultAsync(implicit executionContext: ExecutionContext): DefaultHandlerBuilder.Type[Future, Context] =
//    DefaultHandlerBuilder(system = Some(DefaultEffectSystem.async), protocol = Some(DefaultRpcProtocol()))
//
//  /**
//   * Set default plugins with synchronous effect ''system'' using identity as an effect type.
//   *
//   * @param executionContext execution context
//   * @return RPC request handler builder
//   */
//  def defaultSync: DefaultHandlerBuilder.Type[Identity, Context] =
//    DefaultHandlerBuilder(system = Some(DefaultEffectSystem.sync), protocol = Some(DefaultRpcProtocol()))
//}
//
//object DefaultHandlerBuilder {
//
//  /**
//   * Default request handler type.
//   *
//   * @tparam Effect effect type
//   * @tparam Context request context type
//   */
//  type Type[Effect[_], Context] = DefaultHandlerBuilder[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect, Context]
//}
