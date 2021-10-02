package test.codec

import automorph.spi.MessageCodec
import java.nio.charset.StandardCharsets
import org.scalacheck.Arbitrary
import test.base.BaseSpec

/**
 * Message codec test.
 *
 * Checks message serialization, deserialization and textual representation.
 *
 * @tparam Node message node type
 * @tparam ActualCodec message codec
 */
trait MessageCodecSpec extends BaseSpec {

  type Node
  type ActualCodec <: MessageCodec[Node]

  def codec: ActualCodec

  implicit def arbitraryNode: Arbitrary[Node]

  private val charset = StandardCharsets.UTF_8

  "" - {
    "Format" - {
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
}
