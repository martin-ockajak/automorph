//package test.format.messagepack
//
//import automorph.format.messagepack.UpickleMessagePackFormat
//import automorph.format.UpickleCustom
//import automorph.spi.{Message, MessageError}
//import org.scalacheck.{Arbitrary, Gen}
//import scala.annotation.nowarn
//import scala.collection.mutable.LinkedHashMap
//import test.Generators.arbitraryRecord
//import test.format.MessageFormatSpec
//import test.{Enum, Record, Structure}
//import upack.{Arr, Bool, Float64, Msg, Obj, Str}
//
//class UpickleMessagePackSpec extends MessageFormatSpec {
//
//  type Node = Msg
//  type ActualFormat = UpickleMessagePackFormat[UpickleMessagePackSpec.type]
//
//  override def format: ActualFormat = UpickleMessagePackFormat(UpickleMessagePackSpec)
//
//  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
//    Gen.oneOf(
//      Gen.resultOf(Str(_)),
//      Gen.resultOf(Float64(_)),
//      Gen.resultOf(Bool(_)),
//      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
//      Gen.mapOfN(2, Gen.zip(Gen.resultOf[String, Msg](Str(_)), recurse)).map(values => Obj(LinkedHashMap.from(values)))
//    )
//  ))
//
//  private lazy val custom = format.custom
//  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW
//  Seq(recordRw)
//
//  "" - {
//    "Test" in {
//      val messsage = Message[Node](Some(""),Some(Left(0E-181)),Some(""),Some(Right(Map())),None,Some(MessageError(Some(""),Some(1),Some(Obj(LinkedHashMap(Str("") -> Str("")))))))
//      println(format.format(messsage))
//      val formedMesage = format.deserialize(format.serialize(messsage))
//      println(messsage)
//      println(formedMesage)
//    }
//    "Encode / Decode" in {
//      check { (record: Record) =>
//        val encodedValue = format.encode(record)
//        val decodedValue = format.decode[Record](encodedValue)
//        decodedValue.equals(record)
//      }
//    }
//  }
//}
//
//object UpickleMessagePackSpec extends UpickleCustom {
//
//  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
//    value => Enum.toOrdinal(value),
//    number => Enum.fromOrdinal(number)
//  )
//
//  @nowarn
//  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
//}
