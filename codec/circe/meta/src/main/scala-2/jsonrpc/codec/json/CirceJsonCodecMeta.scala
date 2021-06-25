package jsonrpc.codec.json

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

  override def encode[T](value: T): Json = CirceJsonCodecMeta.encode(custom, value)

  override def decode[T](node: Json): T = CirceJsonCodecMeta.decode(custom, value)
}

private[jsonrpc] object CirceJsonCodecMeta {

  def encode[Custom <: UpickleCustom, T](custom: Custom, value: T): Json = macro encodeExpr[Custom, T]

  def encodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], value: c.Expr[T]): c.Expr[Json] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[Json](q"""
      val encoder = implicitly[$custom.CirceEncoder[$valueType]].encoder
      $value.asJson(encoder)
    """)
  }

  def decode[Custom <: UpickleCustom, T](custom: Custom, node: Json): T = macro decodeExpr[Custom, T]

  def decodeExpr[Custom: c.WeakTypeTag, T: c.WeakTypeTag](c: Context)(custom: c.Expr[Custom], node: c.Expr[Json]): c.Expr[T] = {
    import c.universe.{weakTypeOf, Quasiquote}

    val valueType = weakTypeOf[T]
    c.Expr[T](q"""
      val decoder = implicitly[$custom.CirceDecoder[$valueType]].decoder
      $node.as[$valueType](decoder).toTry.get
    """)
  }
}
