package test

import org.scalacheck.Arbitrary.{
  arbBool, arbByte, arbDouble, arbFloat, arbInt, arbLong, arbOption, arbShort, arbString, arbitrary,
}
import org.scalacheck.Arbitrary
import org.scalacheck.Gen.{asciiPrintableStr, choose, listOf, mapOf, zip}
import test.Enum.Enum

case object Generators {

  implicit val arbitraryEnum: Arbitrary[Enum] = Arbitrary(choose(0, Enum.values.size - 1).map(Enum.fromOrdinal))

  implicit val arbitraryStructure: Arbitrary[Structure] = Arbitrary(for {
    value <- asciiPrintableStr
  } yield Structure(value))

  implicit val arbitraryRecord: Arbitrary[Record] = {
    Arbitrary(for {
      string <- asciiPrintableStr
      boolean <- arbitrary[Boolean]
      byte <- arbitrary[Byte]
      short <- arbitrary[Short]
      int <- arbitrary[Option[Int]]
      long <- arbitrary[Long]
      float <- arbitrary[Float]
      double <- arbitrary[Double]
      enumeration <- arbitrary[Enum]
      list <- listOf(asciiPrintableStr)
      map <- mapOf(zip(asciiPrintableStr, arbitrary[Int]))
      structure <- arbitrary[Option[Structure]]
      none <- arbitrary[Option[String]]
    } yield Record(string, boolean, byte, short, int, long, float, double, enumeration, list, map, structure, none))
  }
}
