package jsonrpc.codec

import base.BaseSpec
import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message.{Params, version}
import jsonrpc.spi.{Codec, Message, MessageError}
import jsonrpc.Generators.arbitraryMesage
import org.scalacheck.Arbitrary

/**
 * Codec test.
 *
 * Checks message serialization, deserialization and formatting.
 */
trait CodecSpec extends BaseSpec:

  private lazy val charset = StandardCharsets.UTF_8

  type Node
  type CodecType <: Codec[Node]

  def codec: CodecType

  def arbitraryNode: Arbitrary[Node]

  "" - {
    given Arbitrary[Message[Node]] = arbitraryMesage(using arbitraryNode)
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
