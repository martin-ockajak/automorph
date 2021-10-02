package test

import org.scalacheck.Arbitrary.{arbBool, arbByte, arbDouble, arbFloat, arbInt, arbLong, arbOption, arbShort, arbString, arbitrary}
import org.scalacheck.{Arbitrary, Gen}
import test.Enum.Enum

object Generators {

  implicit val arbitraryEnum: Arbitrary[Enum] = Arbitrary(Gen.choose(0, Enum.values.size - 1).map(Enum.fromOrdinal))

  implicit val arbitraryStructure: Arbitrary[Structure] = Arbitrary(for {
    value <- arbitrary[String]
  } yield Structure(value))

  implicit val arbitraryRecord: Arbitrary[Record] = Arbitrary(for {
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
  } yield Record(string, boolean, byte, short, int, long, float, double, enumeration, list, map, structure, none))
}
