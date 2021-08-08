package test.format

import java.nio.charset.StandardCharsets
import automorph.spi.{Message, MessageFormat}
import org.scalacheck.Arbitrary
import test.Generators
import test.base.BaseSpec

/**
 * Message format test.
 *
 * Checks message serialization, deserialization and formatting.
 */
trait MessageFormatSpec extends BaseSpec {

  type Node
  type ActualFormat <: MessageFormat[Node]

  def format: ActualFormat

  implicit def arbitraryNode: Arbitrary[Node]

  val charset = StandardCharsets.UTF_8
  implicit lazy val arbitraryMessage: Arbitrary[Message[Node]] = Generators.arbitraryMesage

  "" - {
//    "Serialize / Deserialize" in {
//      check { (message: Message[Node]) =>
//        val rawMessage = format.serialize(message)
//        val formedMessage = format.deserialize(rawMessage)
//        formedMessage.equals(message)
//      }
//    }
    "Format" in {
      check { (message: Message[Node]) =>
        val formattedMessage = format.format(message)
        val rawMessage = format.serialize(message)
        formattedMessage.getBytes(charset).length > rawMessage.size
      }
    }
  }
}
