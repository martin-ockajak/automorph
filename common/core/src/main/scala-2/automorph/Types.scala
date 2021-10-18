package automorph

import automorph.Handler
import automorph.spi.MessageCodec

/** Common type aliases. */
object Types {

  /** Handler with arbitrary node and codec type. */
  type HandlerAnyCodec[Effect[_], Context] = Any

  /** Client with arbitrary node and codec type. */
  type ClientAnyCodec[Effect[_], Context] = Any

  /** Handler without codec */
  type HandlerGenericCodec[Effect[_], Context] = Handler[Unit, MessageCodec[Unit], Effect, Context]

  /** Client without codec */
  type ClientGenericCodec[Effect[_], Context] = Client[Unit, MessageCodec[Unit], Effect, Context]
}
