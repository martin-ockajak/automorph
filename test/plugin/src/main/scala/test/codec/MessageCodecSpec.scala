package test.codec

import java.nio.charset.StandardCharsets
import automorph.spi.MessageCodec
import org.scalacheck.Arbitrary
import test.Generators
import test.base.BaseSpec

/**
 * Message codec test.
 *
 * Checks message serialization, deserialization and textual representation.
 */
trait MessageCodecSpec extends BaseSpec {

  type Node
  type ActualCodec <: MessageCodec[Node]

  def codec: ActualCodec

  implicit def arbitraryNode: Arbitrary[Node]

  val charset = StandardCharsets.UTF_8

  "" - {
    "Serialize / Deserialize" in {
      check { (node: Node) =>
        val serializedNode = codec.serialize(node)
        codec.deserialize(serializedNode).equals(node)
      }
    }
    "Text" in {
      check { (node: Node) =>
        val textNode = codec.text(node)
        val serializedNode = codec.serialize(node)
        textNode.getBytes(charset).length >= serializedNode.length
      }
    }
  }
}
