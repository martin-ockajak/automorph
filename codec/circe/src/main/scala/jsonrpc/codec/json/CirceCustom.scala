package jsonrpc.codec.json

import io.circe.{Decoder, Encoder}
import scala.language.implicitConversions

trait CirceCustom {

  final case class CirceEncoder[T](encoder: Encoder[T])
  final case class CirceDecoder[T](decoder: Decoder[T])

  implicit def encoderToCirceEncoder[T](encoder: Encoder[T]): CirceEncoder[T] = CirceEncoder(encoder)

  implicit def encoderToCirceDecoder[T](decoder: Decoder[T]): CirceDecoder[T] = CirceDecoder(decoder)
}
