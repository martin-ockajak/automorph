package test.codec.json

import com.fasterxml.jackson.databind.ObjectMapper
import test.codec.MessageCodecSpec

/**
 * JSON message codec test.
 *
 * Checks message serialization.
 *
 * @tparam Node message node type
 * @tparam ActualCodec message codec
 */
trait JsonMessageCodecSpec extends MessageCodecSpec {
  private val objectMapper = new ObjectMapper()

  "" - {
    "JSON" - {
      "Serialize" in {
        check { (node: Node) =>
          val serialized = codec.serialize(node)
          objectMapper.readTree(serialized.unsafeArray)
          true
        }
      }
      "Text" in {
        check { (node: Node) =>
          val text = codec.text(node)
          objectMapper.readTree(text)
          true
        }
      }
    }
  }
}
