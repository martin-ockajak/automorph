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
          val deserialized = objectMapper.readTree(serialized)
          deserialized.shouldEqual(node)
        }
      }
      "Text" in {
        forAll { (node: Node) =>
          val text = codec.text(node)
          val deserialized  = objectMapper.readTree(text)
          deserialized.shouldEqual(node)
        }
      }
    }
  }
}
