package automorph.codec.json

import argonaut.Argonaut.{jNull, StringToParseWrap}
import argonaut.{CodecJson, DecodeResult, Json}
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

/**
 * Argonaut JSON message codec plugin.
 *
 * @see [[https://www.json.org Format]]
 * @see [[http://argonaut.io/doc Documentation]]
 * @see [[http://argonaut.io/scaladocs/#argonaut.Json Node type]]
 * @constructor Creates an Argonaut codec plugin using JSON as message format.
 */
final case class ArgonautJsonCodec() extends ArgonautJsonMeta {

  override def mediaType: String = "application/json"

  override def serialize(node: Json): ArraySeq.ofByte =
    new ArraySeq.ofByte(node.nospaces.getBytes(ArgonautJsonCodec.charset))

  override def deserialize(data: ArraySeq.ofByte): Json =
    new String(data.unsafeArray, ArgonautJsonCodec.charset).decodeEither[Json].fold(
      errorMessage => throw new IllegalArgumentException(errorMessage),
      identity
    )

  override def text(node: Json): String =
    node.spaces2
}

object ArgonautJsonCodec {

  /** Message node type. */
  type Node = Json

  implicit lazy val noneCodecJson: CodecJson[None.type] = CodecJson(
    (_: None.type) => jNull,
    cursor => if (cursor.focus.isNull) DecodeResult.ok(None) else DecodeResult.fail("Not a null", cursor.history)
  )

  implicit lazy val jsonRpcMessageCodecJson: CodecJson[ArgonautJsonRpc.RpcMessage] =
    ArgonautJsonRpc.messageCodecJson

  implicit lazy val restRpcMessageCodecJson: CodecJson[ArgonautRestRpc.RpcMessage] =
    ArgonautRestRpc.messageCodecJson

  private val charset = StandardCharsets.UTF_8
}
