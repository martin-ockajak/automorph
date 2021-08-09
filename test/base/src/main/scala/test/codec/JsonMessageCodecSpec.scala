package test.codec

import automorph.spi.Message

/**
 * JSON message codec test.
 *
 * Checks message serialization, deserialization and codecting.
 */
trait JsonMessageCodecSpec extends MessageCodecSpec {

  "" - {
    "Codec" - {
      "JSON-RPC" - {
        "Request - String Id / Named Parameters" in {
          val message = Message[Node](Some("2.0"), Some(Right("test")), Some("test"), Some(Right(Map.empty)), None, None)
          val expectedMessage = """{"jsonrpc":"2.0","id":"test","method":"test","params":{}}"""
          serialized(message).should(equal(expectedMessage))
        }
        "Request - Numeric Id / Named Parameters" in {
          val message = Message[Node](Some("2.0"), Some(Left(0)), Some("test"), Some(Right(Map.empty)), None, None)
          val expectedMessage = """{"jsonrpc":"2.0","id":0,"method":"test","params":{}}"""
          serialized(message).should(equal(expectedMessage))
        }
        "Request - String Id / Positional Parameters" in {
          val message = Message[Node](Some("2.0"), Some(Right("test")), Some("test"), Some(Left(List.empty)), None, None)
          val expectedMessage = """{"jsonrpc":"2.0","id":"test","method":"test","params":[]}"""
          serialized(message).should(equal(expectedMessage))
        }
        "Request - Numeric Id / Positional Parameters" in {
          val message = Message[Node](Some("2.0"), Some(Left(0)), Some("test"), Some(Left(List.empty)), None, None)
          val expectedMessage = """{"jsonrpc":"2.0","id":0,"method":"test","params":[]}"""
          serialized(message).should(equal(expectedMessage))
        }
      }
    }
  }

  private def serialized(message: Message[Node]): String = new String(codec.serialize(message).unsafeArray, charset)
}
