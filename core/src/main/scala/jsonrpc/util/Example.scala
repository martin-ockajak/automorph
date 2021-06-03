//package jsonrpc.util
//
//import scala.quoted.{Expr, Quotes, Type}
//
//trait Parser:
//  // Plugin is a path-dependent type
//  trait Plugin[T]:
//    def parse(text: String): T
//
//  // Invokes a macro requiring a given Plugin[T]
//  def parse[T: Plugin](text: String): T =
//    summon[Plugin[T]].parse(text)
//
//class ParserImpl extends Parser:
//  // Dummy int plugin implementation - only its type matters
//  given intPlugin: Plugin[Int] = new Plugin[Int]:
//    def parse(text: String): Int = 0
//
//object Macros:
//  inline def processMacro[P <: Parser, T](inline parser: Parser, inline text: String): T = ${ processMacro('parser, 'text) }
//
//  private def processMacro[P <: Parser: Type, T: Type](
//    parser: Expr[Parser],
//    text: Expr[String]
//  )(using
//    quotes: Quotes
//  ): Expr[String] =
//    '{
//      val theParser = $parser
//      // Compile error: no implicit argument of type theParser.Plugin[T] was found for an implicit parameter of method parse in trait Parser
//      theParser.parse($text)
//
//      // A delayed summon construct equivalent to this code is needed
//      // theParser.parse($text)(using summonInline[theParser.Parser[T]])
//    }
//
//trait Api:
//  def process[T](text: String): T
//
//final case class ApiImpl(parser: Parser) extends Api:
//  override def process[T](text: String): T =
//    Macros.processMacro(parser, text)
