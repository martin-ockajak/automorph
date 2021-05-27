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
//  private given CanEqual[TextNode, String] = CanEqual.derived
//
//  override def codec: Codec[JsonNode] = JacksonJsonCodec()
//
//  override def messageArguments: Seq[Params[JsonNode]] = Seq(
//    Map(
//      "x" -> TextNode("foo"),
//      "y" -> IntNode(1),
//      "z" -> BooleanNode.TRUE
//    ).asRight
//  )
//
//  override def messageResults: Seq[JsonNode] = Seq(
//    TextNode("test")
//  )
//
//  "" - {
//    "Deserialize" in {
//      val x = codec.deserialize(codec.serialize(messages.head))
//      println(messages.head)
//      println(x)
//      println(messages.head.result.get.getClass.getName)
//      println(x.result.get.getClass.getName)
//      codec.serialize(x)
//    }
//  }
