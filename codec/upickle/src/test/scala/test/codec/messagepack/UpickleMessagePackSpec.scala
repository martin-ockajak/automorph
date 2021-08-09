//package test.codec.messagepack
//
//import automorph.codec.messagepack.UpickleMessagePackCodec
//import automorph.codec.UpickleCustom
//import automorph.spi.{Message, MessageError}
//import org.scalacheck.{Arbitrary, Gen}
//import scala.annotation.nowarn
//import scala.collection.mutable.LinkedHashMap
//import test.Generators.arbitraryRecord
//import test.codec.MessageCodecSpec
//import test.{Enum, Record, Structure}
//import upack.{Arr, Bool, Float64, Msg, Obj, Str}
//
//class UpickleMessagePackSpec extends MessageCodecSpec {
//
//  type Node = Msg
//  type ActualCodec = UpickleMessagePackCodec[UpickleMessagePackSpec.type]
//
//  override def codec: ActualCodec = UpickleMessagePackCodec(UpickleMessagePackSpec)
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
//  private lazy val custom = codec.custom
//  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW
//  Seq(recordRw)
//
//  "" - {
//    "Test" in {
//      val messsage = Message[Node](Some(""),Some(Left(0E-181)),Some(""),Some(Right(Map())),None,Some(MessageError(Some(""),Some(1),Some(Obj(LinkedHashMap(Str("") -> Str("")))))))
//      println(codec.codec(messsage))
//      val formedMesage = codec.deserialize(codec.serialize(messsage))
//      println(messsage)
//      println(formedMesage)
//    }
//    "Encode / Decode" in {
//      check { (record: Record) =>
//        val encodedValue = codec.encode(record)
//        val decodedValue = codec.decode[Record](encodedValue)
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