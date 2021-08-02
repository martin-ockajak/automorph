package test

import automorph.spi.{Message, MessageError}
import org.scalacheck.Arbitrary.{arbBool, arbByte, arbContainer, arbDouble, arbFloat, arbInt, arbLong, arbOption, arbShort, arbString, arbitrary}
import org.scalacheck.{Arbitrary, Gen}
import test.Enum.Enum

case object Generators {

  implicit val arbitraryEnum: Arbitrary[Enum] = Arbitrary {
    Gen.choose(0, Enum.values.size - 1).map(Enum.fromOrdinal)
  }

  implicit val arbitraryStructure: Arbitrary[Structure] = Arbitrary {
    for {
      value <- arbitrary[String]
    } yield Structure(value)
  }

  implicit val arbitraryRecord: Arbitrary[Record] = Arbitrary {
    for {
      string <- arbitrary[String]
      boolean <- arbitrary[Boolean]
      byte <- arbitrary[Byte]
      short <- arbitrary[Short]
      int <- arbitrary[Option[Int]]
      long <- arbitrary[Long]
      float <- arbitrary[Float]
      double <- arbitrary[Double]
      enumeration <- arbitrary[Enum]
      list <- arbitrary[List[String]]
      map <- arbitrary[Map[String, Int]]
      structure <- arbitrary[Option[Structure]]
      none <- arbitrary[Option[String]]
    }
    yield Record(string, boolean, byte, short, int, long, float, double, enumeration, list, map, structure, none)
  }

  def arbitraryMesage[Node: Arbitrary]: Arbitrary[Message[Node]] = {
    implicit val arbitraryMessageError: Arbitrary[MessageError[Node]] = Arbitrary {
      for {
        message <- arbitrary[Option[String]]
        code <- arbitrary[Option[Int]]
        data <- arbitrary[Option[Node]]
      } yield MessageError(message, code, data)
    }

    Arbitrary {
      for {
        automorph <- arbitrary[Option[String]]
        id <- arbitrary[Option[Either[BigDecimal, String]]]
        method <- arbitrary[Option[String]]
        params <- arbitrary[Option[Message.Params[Node]]]
        result <- arbitrary[Option[Node]]
        error <- arbitrary[Option[MessageError[Node]]]
      } yield Message(automorph, id, method, params, result, error)
    }
  }
}
