package jsonrpc
import java.lang.reflect.Proxy
import scala.reflect.ClassTag

final case class Invocation(methodName: String, params: Seq[Any])

final case class ClientProxy():
  def instanceFor[T: ClassTag](f: Invocation => Any):T =
    Proxy.newProxyInstance(
      this.getClass.getClassLoader,
      Array( summon[ClassTag[T]].runtimeClass),
      ( proxy, method, params) => f( Invocation( method.getName, params.toSeq))
    ).asInstanceOf[T]


case object ClientProxy:

  def main(args: Array[String]): Unit =
    trait Dog:
      def bark(seconds:Int): String
      def bark(seconds:Int)(happy:Boolean): String
      def sleep(seconds:Int): String

    val proxy = ClientProxy().instanceFor[Dog]{
      case Invocation( "bark", Seq(i)) => s"barking for $i seconds"
      case Invocation( "bark", Seq(i, happy)) => s"barking for $i seconds, happy = $happy"
      case Invocation( "sleep", Seq(i)) => s"sleeping for $i seconds"
      case Invocation(  method, params) => sys.error(s"unknown method $method(${params.mkString(", ")})")
    }

    extension( x:Any) def println() = Console.println(x)

    proxy.bark(1).println()
    proxy.bark(1)(true).println()
    proxy.sleep(200).println()
