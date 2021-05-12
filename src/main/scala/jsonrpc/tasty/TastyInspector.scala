package jsonrpc.tasty

import scala.quoted.*
import scala.tasty.inspector.*

class MyInspector
  extends Inspector:

  def inspect(using Quotes)(tastys: List[Tasty[quotes.type]]): Unit =
    import quotes.reflect.*
    for tasty <- tastys do
      val tree = tasty.ast
      println(tree)
      // Do something with the tree

object Tasty:
  def main(args: Array[String]): Unit =
    val tastyFiles = List("target/scala-3.0.0-RC3/test-classes/jsonrpc/Api.tasty")
    TastyInspector.inspectTastyFiles(tastyFiles)(new MyInspector)