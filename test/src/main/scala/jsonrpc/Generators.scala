package jsonrpc

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.*

case object Generators:

  given Arbitrary[Enum] = Arbitrary {
    Gen.choose(0, Enum.values.size - 1).map(Enum.fromOrdinal)
  }

  given Arbitrary[Structure] = Arbitrary {
    for
      value <- arbitrary[String]
    yield Structure(value)
  }

  given Arbitrary[Record] = Arbitrary {
    for
      string <- arbitrary[String]
      boolean <- arbitrary[Boolean]
      byte <- arbitrary[Byte]
      short <- arbitrary[Short]
      int <- arbitrary[Option[Int]]
      long <- arbitrary[Long]
      float <- arbitrary[Option[Float]]
      double <- arbitrary[Double]
      enumeration <- arbitrary[Enum]
      list <- arbitrary[List[String]]
      map <- arbitrary[Map[String, Int]]
      structure <- arbitrary[Option[Structure]]
      none <- arbitrary[Option[String]]
    yield Record(
      string,
      boolean,
      byte,
      short,
      int,
      long,
      float,
      double,
      enumeration,
      list,
      map,
      structure,
      none
    )
  }
