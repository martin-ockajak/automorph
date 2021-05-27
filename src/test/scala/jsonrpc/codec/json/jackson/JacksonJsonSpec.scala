//package jsonrpc.codec.json.jackson
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.node.{BooleanNode, IntNode, TextNode}
//import jsonrpc.Enum
//import jsonrpc.codec.CodecSpec
//import jsonrpc.codec.json.jackson.JacksonJsonCodec
//import jsonrpc.spi.Message.Params
//import jsonrpc.spi.Codec
//import jsonrpc.util.ValueOps.{asRight, asSome}
//
//class JacksonJsonSpec extends CodecSpec[JsonNode]:
//
//  override def codec: Codec[JsonNode] = JacksonJsonCodec()
//
//  override def messageArguments: Seq[Params[JsonNode]] = Seq(
//    Map(
//      "x" -> TextNode("foo"),
//      "y" -> IntNode(1),
//      "z" -> BooleanNode.TRUE.nn
//    ).asRight
//  )
//
//  override def messageResults: Seq[JsonNode] = Seq(
//    TextNode("test")
//  )
