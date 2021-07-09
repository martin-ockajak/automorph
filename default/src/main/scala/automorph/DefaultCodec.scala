package automorph

import automorph.DefaultTypes.DefaultCodec
import automorph.codec.json.UpickleJsonCodec

case object DefaultCodec {

  /**
   * Creates a default structured message format codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @return codec plugin
   */
  def apply(): DefaultCodec = UpickleJsonCodec()
}
