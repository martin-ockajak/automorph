package automorph

import automorph.Handler
import automorph.spi.{EffectSystem, MessageCodec}

/** Common type aliases. */
object Types:

  /** Handler with arbitrary node and codec type. */
  type HandlerAnyCodec[Effect[_], Context] = Handler[_, _, Effect, Context]

  /** Client with arbitrary node and codec type. */
  type ClientAnyCodec[Effect[_], Context] = Client[_, _, Effect, Context]

  /** Handler without codec */
  type HandlerGenericCodec[Effect[_], Context] = Handler[Unit, MessageCodec[Unit], Effect, Context]

  /** Client without codec */
  type ClientGenericCodec[Effect[_], Context] = Client[Unit, MessageCodec[Unit], Effect, Context]
