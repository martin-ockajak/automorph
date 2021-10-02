package test.codec.json

import automorph.spi.MessageCodec
import automorph.util.Bytes
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import org.scalacheck.Arbitrary
import test.base.BaseSpec
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
          val serializedNode = codec.serialize(node)
          objectMapper.readTree(Bytes.inputStream.to(serializedNode))
          true
        }
      }
      "Text" in {
        check { (node: Node) =>
          val serializedNode = codec.text(node)
          objectMapper.readTree(serializedNode)
          true
        }
      }
    }
  }
}
