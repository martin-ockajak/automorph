package test.codec

import java.nio.charset.StandardCharsets
import automorph.spi.{Message, MessageCodec}
import org.scalacheck.Arbitrary
import test.Generators
import test.base.BaseSpec

/**
 * Message codec test.
 *
 * Checks message serialization, deserialization and codecting.
 */
trait MessageCodecSpec extends BaseSpec {

  type Node
  type ActualCodec <: MessageCodec[Node]

  def codec: ActualCodec

  implicit def arbitraryNode: Arbitrary[Node]

  val charset = StandardCharsets.UTF_8
  implicit lazy val arbitraryMessage: Arbitrary[Message[Node]] = Generators.arbitraryMesage

  "" - {
    "Serialize / Deserialize" in {
      check { (message: Message[Node]) =>
        val rawMessage = codec.serialize(message)
        val formedMessage = codec.deserialize(rawMessage)
        formedMessage.equals(message)
      }
    }
    "Codec" in {
      check { (message: Message[Node]) =>
        val codectedMessage = codec.text(message)
        val rawMessage = codec.serialize(message)
        codectedMessage.getBytes(charset).length > rawMessage.size
      }
    }
  }
}
