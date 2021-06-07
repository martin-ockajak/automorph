package jsonrpc

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import UpickleJsonSpec.Node
import UpickleJsonSpec.CodecType
import jsonrpc.spi.Backend
import jsonrpc.backend.standard.FutureBackend
import jsonrpc.handler.Handler

class FutureUpickleJsonSpec extends UpickleJsonSpec[Future]:
  def backend: Backend[Future] = FutureBackend()

//  def handlerx: Handler[Node, CodecType, Future, Short] = handler
