package jsonrpc.format.json.upickle

import jsonrpc.core.Reflection
import jsonrpc.spi.{CallError, FormatContext, Message}
import ujson.Value
import ujson.Str
import upickle.default.{Writer, Reader}
import upickle.{Api, AttributeTagged}
import scala.quoted.{Expr, Quotes, Type, quotes}
import scala.compiletime.{erasedValue, error, summonInline}
import scala.reflect.ClassTag

object UpickleMacros:
  inline def xencode[A <: Api, T](inline parser: A, inline value: T): Value = ${xencode[A, T]('parser, 'value)}

  private def xencode[A <: Api: Type, T: Type](parser: Expr[A], value: Expr[T])(using quotes: Quotes): Expr[Value] =
    import ref.quotes.reflect.*

    val ref = Reflection(quotes)

    def baseTypes(dataType: TypeRepr): Seq[TypeRepr] =
      (Seq(dataType) ++ ref.baseTypes(dataType))

    def typeName(dataType: TypeRepr): String =
      val index = dataType.show.indexOf('[')
      if index < 0 then
        dataType.show
      else
        dataType.show.substring(0, index).nn

    val parserTypeTree = TypeTree.of[A]
    val writerTypeName = TypeRepr.of[Writer].typeSymbol.name
    val parserFields = ref.fields(parserTypeTree).filter(_.public).map { field =>
      field.dataType -> field.name
    }
    val parserMethods = ref.methods(parserTypeTree).filter(_.public).map { method =>
      method.resultType -> method.name
    }
    val writeMembers = (parserFields ++ parserMethods).flatMap { (dataType, name) =>
      baseTypes(dataType).filter(_.typeSymbol.name == writerTypeName).flatMap {
        case appliedType: AppliedType => appliedType.args match
          case List(typeArgument) => Some(typeArgument)
          case _ => None
        case _ => None
      }.map((typeArgument => typeName(typeArgument.dealias) -> name))
    }.toMap
    val valueType = TypeRepr.of[T]
    val valueWriter = writeMembers.get(typeName(valueType)).getOrElse {
      report.error(s"Cannot find given JSON writer for type ${valueType.show}", Position.ofMacroExpansion)
      throw new IllegalStateException(s"Cannot find given JSON writer for type ${valueType.show}")
    }
    val writer = ref.accessTerm(ref.term(parser), valueWriter)
    println(typeName(valueType))
    println(valueWriter)

    //    val call = reflection.callTerm(ref.term(api), "writeJs", List(valueType), List(List(ref.term(value))))
//    val writer = '{${parser}.StringWriter}
//    val callString = ref.callTerm(ref.term(parser), "writeJs",
//      List(TypeTree.of[String]), List(List(ref.term('{"test"})), List(ref.term(writer))))
//    val typedCall = ref.callTerm(ref.term(parser), "writeJs",
//      List(TypeTree.of[String]), List(List(ref.term('{"test"})), List(ref.term(writer))))
    //    println(call)

//    type Y = Type.of[List[summon[Type[T]]]]
//    type Z = Type.of[List[String]]
//      type X = ClassTag[T]
      type X = ClassTag[T]
      Expr.summon[X] match
        case Some(tag) => tag
        case _ => '{
//          type A = Type.of[List[summon[Type[T]].Underlying]]
          ()
        }
//      Expr.summon[parser.Writer[T]] match
//        case Some(writer) => '{
//          val realParser = $parser
//          realParser.writeJs(${value})(${writer})
//          ()
//        }
//        case _ => '{
//          ()
//        }
      println('{${parser}.StringWriter}.asTerm)
      println(writer)
//      println('{
//        val realParser = $parser
//        ${writer.asExpr}
//        realParser.EitherWriter[String, String]
//        }.asTerm)
    '{
//      type X = Type.of[parser.Writer[T]
      val realParser = $parser
      realParser.writeJs("test")
//      realParser.writeJs(${value})(${writer.asExpr}.asInstanceOf[realParser.Writer[T]])
    }
