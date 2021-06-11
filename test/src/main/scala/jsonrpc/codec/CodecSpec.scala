package jsonrpc.codec

import base.BaseSpec
import java.nio.charset.StandardCharsets
import jsonrpc.spi.Message.{Params, version}
import jsonrpc.spi.{Codec, Message, MessageError}
import jsonrpc.{Enum, Record, Structure}

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

  def messageArguments: Seq[Params[Node]]

  def messageResults: Seq[Node]

  def messages: Seq[Message[Node]] =
    for
      argument <- messageArguments
      result <- messageResults
    yield Message(
      Some(version),
      Some(Right("test")),
      None,
      Some(argument),
      Some(result),
      Some(MessageError(
        Some(0),
        Some("Test error"),
        None
      ))
    )

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
    "Serialize / Deserialize" in {
      messages.foreach { message =>
        val rawMessage = codec.serialize(message)
        val formedMessage = codec.deserialize(rawMessage)
        formedMessage.should(equal(message))
      }
    }
    "Format" in {
      messages.foreach { message =>
        val formattedMessage = codec.format(message)
        val rawMessage = codec.serialize(message)
        formattedMessage.getBytes(charset).length.should(be > rawMessage.size)
      }
    }
  }
