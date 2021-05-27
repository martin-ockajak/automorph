package jsonrpc.codec.json.upickle

import jsonrpc.Enum
import jsonrpc.codec.CodecSpec
import jsonrpc.codec.json.upickle.UpickleJsonCodec
import jsonrpc.spi.Message.Params
import jsonrpc.spi.Codec
import jsonrpc.util.ValueOps.{asRight, asSome}
import ujson.{Bool, Num, Str, Value}
import upickle.default.ReadWriter

class UpickleJsonSpec extends CodecSpec[Value]:

  override def codec: Codec[Value] = UpickleJsonCodec(upickle.default)

  override def messageArguments: Seq[Params[Value]] = Seq(
    Map(
      "x" -> Str("foo"),
      "y" -> Num(1),
      "z" -> Bool(true)
    ).asRight
  )

  override def messageResults: Seq[Value] = Seq(
    Str("test")
  )

  private given enumRw: ReadWriter[Enum] =
    upickle.default.readwriter[Int].bimap[Enum](
      value => value.ordinal,
      number => Enum.fromOrdinal(number)
    )
