package test.codec.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import test.codec.MessageCodecTest

/**
 * JSON message codec test.
 *
 * Checks message serialization.
 */
trait JsonMessageCodecTest extends MessageCodecTest with ScalaCheckPropertyChecks{

  private val objectMapper = new ObjectMapper()

  "" - {
    "JSON" - {
      "Serialize" in {
        check { (node: Node) =>
          val serialized = codec.serialize(node)
          objectMapper.readTree(serialized)
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
