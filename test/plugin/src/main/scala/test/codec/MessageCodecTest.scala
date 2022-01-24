package test.codec

import automorph.spi.MessageCodec
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils
import org.scalacheck.Arbitrary
import test.base.BaseTest

/**
 * Message codec test.
 *
 * Checks message serialization, deserialization and textual representation.
 */
trait MessageCodecTest extends BaseTest {

  type Node
  type ActualCodec <: MessageCodec[Node]

  def codec: ActualCodec

  implicit def arbitraryNode: Arbitrary[Node]

  private val charset = StandardCharsets.UTF_8

  "" - {
    "Serialize & Deserialize" in {
      check { (node: Node) =>
        val serialized = codec.serialize(node)
        codec.deserialize(serialized).equals(node)
      }
    }
    "Text" in {
      check { (node: Node) =>
        val text = codec.text(node)
        val serialized = codec.serialize(node)
        text.getBytes(charset).length >= IOUtils.toByteArray(serialized).length
      }
    }
  }
}
