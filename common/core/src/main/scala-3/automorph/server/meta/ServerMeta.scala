package automorph.server.meta

import automorph.Server
import automorph.handler.BindingHandler
import automorph.handler.meta.HandlerBindingGenerator
import automorph.spi.{MessageCodec, RequestHandler, RpcProtocol, ServerTransport}
import scala.collection.immutable.ListMap

/**
 * Server API method bindings layer.
 *
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   RPC message context type
 */
private[automorph] trait ServerMeta[Node, Codec <: MessageCodec[Node], Effect[_], Context]:

  def transport: ServerTransport[Effect, Context]

  def rpcProtocol: RpcProtocol[Node, Codec, Context]

  def handler: RequestHandler[Effect, Context]

  /**
   * Creates a copy of this server with added RPC bindings for all public methods of the specified API instance.
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
   * If the last parameter of bound method is of `Context` type or returns a context function accepting
   * the `Context` type the server-supplied ''request context'' is passed to the bound method or
   * the returned context function as its last argument.
   *
   * @param api
   *   API instance
   * @tparam Api
   *   API type (only member methods of this type are exposed)
   * @return
   *   RPC request server with specified API bindings
   * @throws IllegalArgumentException
   *   if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api): Server[Node, Codec, Effect, Context] =
    bind(api, Seq(_))

  /**
   * Creates a copy of this server with added RPC bindings for all public methods of the specified API instance.
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
   * If the last parameter of bound method is of `Context` type or returns a context function accepting
   * the `Context` type the server-supplied ''request context'' is passed to the bound method or
   * the returned context function as its last argument.
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
   *   RPC request server with specified API bindings
   * @throws IllegalArgumentException
   *   if invalid public methods are found in the API type
   */
  inline def bind[Api <: AnyRef](api: Api, mapName: String => Iterable[String]): Server[Node, Codec, Effect, Context] =
    val apiBindings = handler match
      case bindingHandler: BindingHandler[Node, Codec, Effect, Context] => bindingHandler.apiBindings
      case _ => Seq.empty
    val newApiBindings = HandlerBindingGenerator.bindings[Node, Codec, Effect, Context, Api](
      rpcProtocol.messageCodec, api
    ).flatMap { binding =>
      mapName(binding.function.name).map(_ -> binding)
    }
    val bindingHandler = BindingHandler(
      transport.effectSystem,
      rpcProtocol,
      ListMap.from(apiBindings ++ newApiBindings)
    )
    Server(transport, rpcProtocol, bindingHandler, bindingHandler.functions)
