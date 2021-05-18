package jsonrpc.json.upickle

import jsonrpc.core.Reflection
import jsonrpc.spi.{CallError, JsonContext, Message}
import ujson.Value
import ujson.Str
import upickle.default.{Writer, Reader, ReadWriter, macroRW}
import upickle.{Api, AttributeTagged}
import scala.quoted.{Expr, Quotes, Type, quotes}

object UpickleMacros:
  inline def xencode[T](inline parser: AttributeTagged, inline value: T): Value = ${xencode[T]('parser, 'value)}

  private def xencode[T: Type](parser: Expr[AttributeTagged], value: Expr[T])(using quotes: Quotes): Expr[Value] =
    import ref.quotes.reflect.*

    val ref = Reflection(quotes)
    val parserTypeTree = TypeTree.of[AttributeTagged]
    val parserMethods = ref.methods(parserTypeTree).filter(_.public)
//    ref.fields(parserTypeTree).foreach { field =>
//      println(field.name)
//      println(field.dataType.show)
//      println(field.typeArguments.map(_.show).mkString(", "))
//      println(ref.baseTypes(field.dataType).map(_.show).mkString(", "))
//    }


//    parserMethods.filter(_.resultType.show.contains("Writer")).foreach { method =>
//      println(s"${method.name}: ${method.resultType.show}")
//    }
    //    val publicMethods = introspection.publicMethods(introspection.ref.TypeTree.of[AttributeTagged])
    //    val publicDescription = publicMethods.map(method => s"${method.name} - ${method.flags}\n").mkString("\n")
    //    println(publicDescription)

    val valueType = TypeTree.of[T]
    //    val call = reflection.callTerm(ref.term(api), "writeJs", List(valueType), List(List(ref.term(value))))
    val writer = '{${parser}.StringWriter}
    val callString = ref.callTerm(ref.term(parser), "writeJs",
      List(TypeTree.of[String]), List(List(ref.term('{"test"})), List(ref.term(writer))))
    //    println(call)
    '{
      val realParser = $parser
      ${callString.asExpr}.asInstanceOf[Value]
//      ${call.asExpr}
//      realParser.writeJs("test")
//      realParser.writeJs[T](${value})
//  Str("test")
    }
