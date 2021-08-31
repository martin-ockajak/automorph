package automorph.codec

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
      override def visitNull(index: Int): None.type = None
    }

  implicit override val BooleanReader: Reader[Boolean] = new SimpleReader[Boolean] {

    override def expectedMsg = "expected boolean"
    override def visitTrue(index: Int) = true
    override def visitFalse(index: Int) = false
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val DoubleReader: Reader[Double] = new SimpleReader[Double] {

    override def expectedMsg = "expected number"
    override def visitString(s: CharSequence, index: Int): Double = s.toString.toDouble
    override def visitInt32(d: Int, index: Int): Double = d.toDouble
    override def visitInt64(d: Long, index: Int): Double = d.toDouble
    override def visitUInt64(d: Long, index: Int): Double = d.toDouble
    override def visitFloat64(d: Double, index: Int): Double = d

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Double =
      s.toString.toDouble
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val IntReader: Reader[Int] = new SimpleReader[Int] {

    override def expectedMsg = "expected number"
    override def visitInt32(d: Int, index: Int): Int = d
    override def visitInt64(d: Long, index: Int): Int = d.toInt
    override def visitUInt64(d: Long, index: Int): Int = d.toInt
    override def visitFloat64(d: Double, index: Int): Int = d.toInt

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Int =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toInt

    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val FloatReader: Reader[Float] = new SimpleReader[Float] {

    override def expectedMsg = "expected number"
    override def visitString(s: CharSequence, index: Int): Float = s.toString.toFloat
    override def visitInt32(d: Int, index: Int): Float = d.toFloat
    override def visitInt64(d: Long, index: Int): Float = d.toFloat
    override def visitUInt64(d: Long, index: Int): Float = d.toFloat
    override def visitFloat32(d: Float, index: Int): Float = d
    override def visitFloat64(d: Double, index: Int): Float = d.toFloat

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Float =
      s.toString.toFloat
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val ShortReader: Reader[Short] = new SimpleReader[Short] {

    override def expectedMsg = "expected number"
    override def visitInt32(d: Int, index: Int): Short = d.toShort
    override def visitInt64(d: Long, index: Int): Short = d.toShort
    override def visitUInt64(d: Long, index: Int): Short = d.toShort
    override def visitFloat64(d: Double, index: Int): Short = d.toShort

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Short =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toShort
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val ByteReader: Reader[Byte] = new SimpleReader[Byte] {

    override def expectedMsg = "expected number"
    override def visitInt32(d: Int, index: Int): Byte = d.toByte
    override def visitInt64(d: Long, index: Int): Byte = d.toByte
    override def visitUInt64(d: Long, index: Int): Byte = d.toByte
    override def visitFloat64(d: Double, index: Int): Byte = d.toByte

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Byte =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toByte
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val CharReader: Reader[Char] = new SimpleReader[Char] {

    override def expectedMsg = "expected char"
    override def visitString(d: CharSequence, index: Int): Char = d.charAt(0)
    override def visitChar(d: Char, index: Int): Char = d
    override def visitInt32(d: Int, index: Int): Char = d.toChar
    override def visitInt64(d: Long, index: Int): Char = d.toChar
    override def visitUInt64(d: Long, index: Int): Char = d.toChar
    override def visitFloat64(d: Double, index: Int): Char = d.toChar

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Char =
      Util.parseIntegralNum(s, decIndex, expIndex, index).toChar
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }

  implicit override val LongReader: Reader[Long] = new SimpleReader[Long] {

    override def expectedMsg = "expected number"
    override def visitString(d: CharSequence, index: Int): Long = upickle.core.Util.parseLong(d, 0, d.length())
    override def visitInt32(d: Int, index: Int): Long = d.toLong
    override def visitInt64(d: Long, index: Int): Long = d
    override def visitUInt64(d: Long, index: Int): Long = d
    override def visitFloat64(d: Double, index: Int): Long = d.toLong

    override def visitFloat64StringParts(s: CharSequence, decIndex: Int, expIndex: Int, index: Int): Long =
      Util.parseIntegralNum(s, decIndex, expIndex, index)
    override def visitNull(index: Int) = throw Abort(expectedMsg + " got null")
  }
}

case object DefaultUpickleCustom extends UpickleCustom
