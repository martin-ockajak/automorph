//package test.codec.json
//
//import automorph.codec.json.UpickleJsonCodec
//import automorph.codec.UpickleCustom
//import org.scalacheck.{Arbitrary, Gen}
//import test.Generators.arbitraryRecord
////import test.codec.JsonMessageCodecSpec
//import test.codec.MessageCodecSpec
//import test.{Enum, Record, Structure}
//import ujson.{Arr, Bool, Num, Obj, Str, Value}
//
////class UpickleJsonSpec extends JsonMessageCodecSpec {
//class UpickleJsonSpec extends MessageCodecSpec {
//
//  type Node = Value
//  type ActualCodec = UpickleJsonCodec[UpickleJsonSpec.type]
//
//  override def codec: ActualCodec = UpickleJsonCodec(UpickleJsonSpec)
//
//  override lazy val arbitraryNode: Arbitrary[Node] = Arbitrary(Gen.recursive[Node](recurse =>
//    Gen.oneOf(
//      Gen.resultOf(Str(_)),
//      Gen.resultOf(Num(_)),
//      Gen.resultOf(Bool(_)),
//      Gen.listOfN[Node](2, recurse).map(Arr(_: _*)),
//      Gen.mapOfN(2, Gen.zip(Arbitrary.arbitrary[String], recurse)).map(Obj.from)
//    )
//  ))
//
//  private lazy val custom = codec.custom
//  implicit private lazy val recordRw: custom.ReadWriter[Record] = custom.macroRW
//
//  "" - {
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
//object UpickleJsonSpec extends UpickleCustom {
//
//  implicit lazy val enumRw: ReadWriter[Enum.Enum] = readwriter[Int].bimap[Enum.Enum](
//    value => Enum.toOrdinal(value),
//    number => Enum.fromOrdinal(number)
//  )
//  implicit lazy val structureRw: ReadWriter[Structure] = macroRW
//}
