package automorph.endpoint.meta

import automorph.Endpoint
import automorph.handler.BindingHandler
import automorph.handler.meta.HandlerGenerator
import automorph.spi.{EndpointTransport, MessageCodec, RequestHandler, RpcProtocol}
import scala.collection.immutable.ListMap

/**
 * Endpoint method bindings code generation.
 *
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   RPC message context type
 * @tparam Adapter
 *   transport layer transport type
 */
private[automorph] trait EndpointMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context, Adapter]:

  def transport: EndpointTransport[Effect, Context, Adapter]

  def rpcProtocol: RpcProtocol[Node, Codec, Context]

  def handler: RequestHandler[Effect, Context]

  /**
   * Creates a copy of this endpoint with added RPC bindings for all public methods of the specified API instance.
   *
   * The binding generation fails if any public API method has one of the following properties:
   *   - does not return the specified effect type
   *   - is overloaded
   *   - has type parameters
   *   - is inline
   *
   * Bindings API methods using the names identical to already existing bindings replaces * the existing bindings
   * with the new bindings.
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting
   * one the endpoint-supplied ''request context'' is passed to the bound method or the returned context function as its
   * last argument.
   *
   * @param api
   *   API instance
   * @tparam Api
   *   API type (only member methods of this type are exposed)
   * @return
   *   RPC request endpoint with specified API bindings
   * @throws IllegalArgumentException
   *   if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api): Endpoint[Node, Codec, Effect, Context, Adapter] =
    bind(api, Seq(_))

  /**
   * Creates a copy of this endpoint with added RPC bindings for all public methods of the specified API instance.
   *
   * The binding generation fails if any public API method has one of the following properties:
   *   - does not return the specified effect type
   *   - is overloaded
   *   - has type parameters
   *   - is inline
   *
   * Bindings API methods using the names identical to already existing bindings replaces the existing bindings
   * with the new bindings.
   *
   * If a bound method definition contains a last parameter of `Context` type or returns a context function accepting
   * one the endpoint-supplied ''request context'' is passed to the bound method or the returned context function as its
   * last argument.
   *
   * Bound API methods are exposed as RPC functions with their names transformed via the `mapName` function.
   *
   * @param api
   *   API instance
   * @param mapName
   *   maps bound API method name to the exposed RPC function names (empty result causes the method not to be exposed)
   * @tparam Api
   *   API type (only member methods of this type are exposed)
   * @return
   *   RPC request endpoint with specified API bindings
   * @throws IllegalArgumentException
   *   if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](
    api: Api, mapName: String => Iterable[String]
  ): Endpoint[Node, Codec, Effect, Context, Adapter] =
    val apiBindings = handler match
      case bindingHandler: BindingHandler[Node, Codec, Effect, Context] => bindingHandler.apiBindings
      case _ => Seq.empty
    val newApiBindings = HandlerGenerator.bindings[Node, Codec, Effect, Context, Api](
      rpcProtocol.messageCodec, api
    ).flatMap { binding =>
      mapName(binding.function.name).map(_ -> binding)
    }
    val bindingHandler = BindingHandler(rpcProtocol, transport.effectSystem, ListMap.from(apiBindings ++ newApiBindings))
    Endpoint(transport, rpcProtocol, bindingHandler, bindingHandler.functions)
