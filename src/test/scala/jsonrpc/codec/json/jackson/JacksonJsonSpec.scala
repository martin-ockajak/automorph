//package jsonrpc.codec.json.jackson
//
//import com.fasterxml.jackson.databind.JsonNode
//import com.fasterxml.jackson.databind.node.{BooleanNode, IntNode, TextNode}
//import jsonrpc.codec.CodecSpec
//import jsonrpc.codec.json.jackson.JacksonJsonCodec
//import jsonrpc.spi.Codec
//import jsonrpc.spi.Message.Params
//import jsonrpc.util.ValueOps.asRight
//
//class JacksonJsonSpec extends CodecSpec:
//
//  type Node = JsonNode
//  type CodecType = JacksonJsonCodec
//
//  def codec: CodecType = JacksonJsonCodec()
//
//  def messageArguments: Seq[Params[JsonNode]] = Seq(
//    Map(
//      "x" -> TextNode("foo"),
//      "y" -> IntNode(1),
//      "z" -> BooleanNode.TRUE
//    ).asRight
//  )
//
//  def messageResults: Seq[JsonNode] = Seq(
//    TextNode("test")
//  )
//
//  "" - {
//    "Encode / Decode" in {
//      val encodedValue = codec.encode(record)
//      val decodedValue = codec.decode[Record](encodedValue)
//      decodedValue.should(equal(record))
//    }
//    "Deserialize" in {
//      val x = codec.deserialize(codec.serialize(messages.head))
//      println(messages.head)
//      println(x)
//      println(messages.head.result.get.getClass.getName)
//      println(x.result.get.getClass.getName)
//      codec.serialize(x)
//    }
//  }
