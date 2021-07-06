package automorph.codec.common

import upickle.AttributeTagged
import upickle.core.{Abort, Util}

/**
 * Customized Upickle reader and writer implicits instance.
 *
 * Contains null-safe readers and writers for basic data types.
 */
trait UpickleCustom extends AttributeTagged {

  implicit override def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]](_.getOrElse(null.asInstanceOf[T]))

  implicit override def OptionReader[T: Reader]: Reader[Option[T]] =
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))) {
      override def visitNull(index: Int) = None
    }

  implicit override val BooleanReader: Reader[Boolean] = new SimpleReader[Boolean] {

    override def expectedMsg = "expected boolean"
    override def visitTrue(index: Int) = true
    override def visitFalse(index: Int) = false
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val DoubleReader: Reader[Double] = new SimpleReader[Double] {

    override def expectedMsg = "expected number"
    override def visitString(s: CharSequence, index: Int) = s.toString.toDouble
    override def visitInt32(d: Int, index: Int) = d.toDouble
    override def visitInt64(d: Long, index: Int) = d.toDouble
    override def visitUInt64(d: Long, index: Int) = d.toDouble
    override def visitFloat64(d: Double, index: Int) = d

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      s.toString.toDouble
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val IntReader: Reader[Int] = new SimpleReader[Int] {

    override def expectedMsg = "expected number"
    override def visitInt32(d: Int, index: Int) = d
    override def visitInt64(d: Long, index: Int) = d.toInt
    override def visitUInt64(d: Long, index: Int) = d.toInt
    override def visitFloat64(d: Double, index: Int) = d.toInt

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toInt

    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val FloatReader: Reader[Float] = new SimpleReader[Float] {

    override def expectedMsg = "expected number"
    override def visitString(s: CharSequence, index: Int) = s.toString.toFloat
    override def visitInt32(d: Int, index: Int) = d.toFloat
    override def visitInt64(d: Long, index: Int) = d.toFloat
    override def visitUInt64(d: Long, index: Int) = d.toFloat
    override def visitFloat32(d: Float, index: Int) = d
    override def visitFloat64(d: Double, index: Int) = d.toFloat

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      s.toString.toFloat
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val ShortReader: Reader[Short] = new SimpleReader[Short] {

    override def expectedMsg = "expected number"
    override def visitInt32(d: Int, index: Int) = d.toShort
    override def visitInt64(d: Long, index: Int) = d.toShort
    override def visitUInt64(d: Long, index: Int) = d.toShort
    override def visitFloat64(d: Double, index: Int) = d.toShort

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toShort
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val ByteReader: Reader[Byte] = new SimpleReader[Byte] {

    override def expectedMsg = "expected number"
    override def visitInt32(d: Int, index: Int) = d.toByte
    override def visitInt64(d: Long, index: Int) = d.toByte
    override def visitUInt64(d: Long, index: Int) = d.toByte
    override def visitFloat64(d: Double, index: Int) = d.toByte

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toByte
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val CharReader: Reader[Char] = new SimpleReader[Char] {

    override def expectedMsg = "expected char"
    override def visitString(d: CharSequence, index: Int) = d.charAt(0)
    override def visitChar(d: Char, index: Int) = d
    override def visitInt32(d: Int, index: Int) = d.toChar
    override def visitInt64(d: Long, index: Int) = d.toChar
    override def visitUInt64(d: Long, index: Int) = d.toChar
    override def visitFloat64(d: Double, index: Int) = d.toChar

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toChar
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }

  implicit override val LongReader: Reader[Long] = new SimpleReader[Long] {

    override def expectedMsg = "expected number"
    override def visitString(d: CharSequence, index: Int) = upickle.core.Util.parseLong(d, 0, d.length())
    override def visitInt32(d: Int, index: Int) = d.toLong
    override def visitInt64(d: Long, index: Int) = d.toLong
    override def visitUInt64(d: Long, index: Int) = d.toLong
    override def visitFloat64(d: Double, index: Int) = d.toLong

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int) =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toLong
    override def visitNull(index: Int) = throw new Abort(expectedMsg + " got null")
  }
}
