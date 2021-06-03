package jsonrpc.codec

import base.BaseSpec
import jsonrpc.core.Protocol
import jsonrpc.util.EncodingOps.toArraySeq
import jsonrpc.spi.Message.Params
import jsonrpc.spi.{Codec, Message, MessageError}
import jsonrpc.util.ValueOps.{asRight, asSome}
import jsonrpc.{Enum, Record, Structure}

trait CodecSpec extends BaseSpec:

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
      Protocol.version.asSome,
      "test".asRight.asSome,
      None,
      argument.asSome,
      result.asSome,
      MessageError(
        Protocol.ErrorType.ApplicationError.code.asSome,
        "Test error".asSome,
        None
      ).asSome
    )

  val record = Record(
    "test",
    boolean = true,
    0,
    1,
    2.asSome,
    3,
    None,
    6.7,
    Enum.One.asSome,
    List("x", "y", "z"),
    Map(
      "foo" -> 0,
      "bar" -> 1
    ),
    Structure(
      "test"
    ).asSome,
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
        formattedMessage.toArraySeq.size.should(be > rawMessage.size)
      }
    }
  }
