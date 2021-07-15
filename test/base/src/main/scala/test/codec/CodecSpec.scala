package test.codec

import java.nio.charset.StandardCharsets
import automorph.spi.{MessageFormat, Message}
import org.scalacheck.Arbitrary
import test.Generators
import test.base.BaseSpec

/**
 * Format test.
 *
 * Checks message serialization, deserialization and formatting.
 */
trait FormatSpec extends BaseSpec {

  type Node
  type ActualFormat <: MessageFormat[Node]

  def codec: ActualFormat

  implicit def arbitraryNode: Arbitrary[Node]

  implicit lazy val arbitraryMessage: Arbitrary[Message[Node]] = Generators.arbitraryMesage
  private lazy val charset = StandardCharsets.UTF_8

  "" - {

    "Serialize / Deserialize" in {
      check { (message: Message[Node]) =>
        val rawMessage = codec.serialize(message)
        val formedMessage = codec.deserialize(rawMessage)
        formedMessage.equals(message)
      }
    }
    "Format" in {
      check { (message: Message[Node]) =>
        val formattedMessage = codec.format(message)
        val rawMessage = codec.serialize(message)
        formattedMessage.getBytes(charset).length > rawMessage.size
      }
    }
  }
}
