package test.codec

import automorph.spi.MessageCodec
import java.nio.charset.StandardCharsets
import org.scalacheck.Arbitrary
import test.{Enum, Generators, Record, Structure}
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

//  private val record = Record(
//    "test",
//    true,
//    0,
//    1,
//    Some(2),
//    3,
//    4.5,
//    6.7,
//    Enum.Enum.One,
//    List("foo", "bar"),
//    Map("a" -> 0, "b" -> 1),
//    Some(Structure("")),
//    None
//  )

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
