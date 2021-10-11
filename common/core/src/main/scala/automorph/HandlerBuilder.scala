//package automorph
//
//import automorph.handler.{HandlerBinding, HandlerCore, HandlerMeta}
//import automorph.log.Logging
//import automorph.spi.protocol.RpcFunction
//import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
//import automorph.util.{CannotEqual, EmptyContext}
//import scala.collection.immutable.ListMap
//
///**
// * Automorph RPC request handler.
// *
// * Used by an RPC server to invoke bound API methods based on incoming RPC requests.
// *
// * @constructor Creates a new RPC request handler with specified request `Context` type plus specified ''codec'' and ''system'' plugins.
// * @param codec message codec plugin
// * @param system effect system plugin
// * @param protocol RPC protocol
// * @param bindings API method bindings
// * @tparam Node message node type
// * @tparam Codec message codec plugin type
// * @tparam Effect effect type
// * @tparam Context request context type
// */
//final case class HandlerBuilder[Node, Codec <: MessageCodec[Node], Effect[_], Context] (
//  codec: Option[Codec],
//  system: Option[EffectSystem[Effect]],
//  protocol: Option[RpcProtocol[Node]]
//) extends CannotEqual
