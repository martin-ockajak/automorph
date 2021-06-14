package jsonrpc.codec.json

import io.circe.syntax.EncoderOps
import io.circe.Json
import jsonrpc.spi.Codec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

/**
 * Circe JSON codec plugin code generation.
 *
 * @tparam Custom customized Circe encoders and decoders implicits instance type
 */
private[jsonrpc] trait CirceJsonCodecMeta[Custom <: CirceCustom] extends Codec[Json] {
  this: CirceJsonCodec[Custom] =>

  override def encode[T](value: T): Json = macro CirceJsonCodecMeta.encode[Custom, T]

  override def decode[T](node: Json): T = macro CirceJsonCodecMeta.decode[Custom, T]
}

private[jsonrpc] object CirceJsonCodecMeta {
  def encode[Custom: c.TypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], value: c.Expr[T]): c.Expr[Json] = {
    import c.universe._

    val valueType = weakTypeOf[T]
    c.Expr[Json](q"""
      val encoder = implicitly[$custom.CirceEncoder[$valueType]].encoder
      $value.asJson(encoder)
    """)
  }

  def decode[Custom: c.TypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], node: c.Expr[Json]): c.Expr[T] = {
    import c.universe._

    val valueType = weakTypeOf[T]
    c.Expr[Json](q"""
      val decoder = implicitly[$custom.CirceDecoder[$valueType]].decoder
      $node.as[$valueType](decoder).toTry.get
    """)
  }
}
