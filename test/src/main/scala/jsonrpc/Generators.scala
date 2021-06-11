package jsonrpc

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.*
import jsonrpc.spi.{Message, MessageError}

case object Generators:

  given arbitraryEnum: Arbitrary[Enum] = Arbitrary {
    Gen.choose(0, Enum.values.size - 1).map(Enum.fromOrdinal)
  }

  given arbitraryStructure: Arbitrary[Structure] = Arbitrary {
    for
      value <- arbitrary[String]
    yield Structure(value)
  }

  given arbitraryRecord: Arbitrary[Record] = Arbitrary {
    for
      string <- arbitrary[String]
      boolean <- arbitrary[Boolean]
      byte <- arbitrary[Byte]
      short <- arbitrary[Short]
      int <- arbitrary[Option[Int]]
      long <- arbitrary[Long]
      double <- arbitrary[Double]
      enumeration <- arbitrary[Enum]
      list <- arbitrary[List[String]]
      map <- arbitrary[Map[String, Int]]
      structure <- arbitrary[Option[Structure]]
      none <- arbitrary[Option[String]]
    yield Record(string, boolean, byte, short, int, long, double, enumeration, list, map, structure, none)
  }

  def arbitraryMesage[Node: Arbitrary]: Arbitrary[Message[Node]] =
    given Arbitrary[MessageError[Node]] = Arbitrary {
      for
        code <- arbitrary[Option[Int]]
        message <- arbitrary[Option[String]]
        data <- arbitrary[Option[Node]]
      yield MessageError(code, message, data)
    }
    Arbitrary {
      for
        jsonrpc <- arbitrary[Option[String]]
        id <- arbitrary[Option[Either[BigDecimal, String]]]
        method <- arbitrary[Option[String]]
        params <- arbitrary[Option[Message.Params[Node]]]
        result <- arbitrary[Option[Node]]
        value <- arbitrary[String]
        error <- arbitrary[Option[MessageError[Node]]]
      yield Message(jsonrpc, id, method, params, result, error)
    }
