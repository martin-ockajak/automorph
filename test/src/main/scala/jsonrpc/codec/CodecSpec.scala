package jsonrpc.codec

import base.BaseSpec
import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message.{Params, version}
import jsonrpc.spi.{Codec, Message, MessageError}
import jsonrpc.{Enum, Generators, Record, Structure}
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

  val record: Record = Record(
    "test",
    boolean = true,
    0,
    1,
    Some(2),
    3,
    None,
    6.7,
    Enum.One,
    List("x", "y", "z"),
    Map(
      "foo" -> 0,
      "bar" -> 1
    ),
    Some(Structure(
      "test"
    )),
    None
  )

  "" - {
    given Arbitrary[Message[Node]] = Generators.arbitraryMesage(using arbitraryNode)
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
