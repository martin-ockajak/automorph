package test.codec.json

import com.fasterxml.jackson.databind.ObjectMapper
import test.codec.MessageCodecTest

/**
 * JSON message codec test.
 *
 * Checks message serialization.
 */
trait JsonMessageCodecTest extends MessageCodecTest {

  private val objectMapper = new ObjectMapper()

  "" - {
    "JSON" - {
      "Serialize" in {
        forAll { (node: Node) =>
          val serialized = codec.serialize(node)
          objectMapper.readTree(if (serialized.hasArray) {
            serialized.array
          } else {
            val array = Array.ofDim[Byte](serialized.remaining)
            serialized.get(array)
            array
          })
        }
      }
      "Text" in {
        forAll { (node: Node) =>
          val text = codec.text(node)
          objectMapper.readTree(text)
        }
      }
    }
  }
}
