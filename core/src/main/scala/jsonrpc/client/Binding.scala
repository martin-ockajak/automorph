package jsonrpc.client

import jsonrpc.client.ClientMeta
import jsonrpc.spi.Codec

sealed trait Binding[Node, CodecType <: Codec[Node], Effect[_], Context]:
  def methodName: String

case class UnnamedBinding[Node, CodecType <: Codec[Node], Effect[_], Context](
  client: ClientMeta[Node, CodecType, Effect, Context, ?],
  methodName: String
) extends Binding[Node, CodecType, Effect, Context]:

  inline def call[A <: Product, R](arguments: A)(using context: Context): Effect[R] =
    client.callByName[A, R](methodName)(arguments)

  inline def notify[A <: Product](arguments: A)(using context: Context): Effect[Unit] =
    client.notifyByName[A](methodName)(arguments)

  inline def parameters(parameterNames: String*): NamedBinding[Node, CodecType, Effect, Context] =
    NamedBinding(client, methodName, parameterNames)

  inline def positional: PositionalBinding[Node, CodecType, Effect, Context] =
    PositionalBinding(client, methodName)

  inline def unnamed: UnnamedBinding[Node, CodecType, Effect, Context] =
    UnnamedBinding(client, methodName)

case class NamedBinding[Node, CodecType <: Codec[Node], Effect[_], Context](
  client: ClientMeta[Node, CodecType, Effect, Context, ?],
  methodName: String,
  parameterNames: Seq[String]
) extends Binding[Node, CodecType, Effect, Context]:

  inline def call[R](arguments: Tuple)(using context: Context): Effect[R] =
    client.callByName[R](methodName)(parameterNames*)(arguments)(using context)

  inline def notify(arguments: Tuple)(using context: Context): Effect[Unit] =
    client.notifyByName(methodName)(parameterNames*)(arguments)(using context)

  inline def positional: PositionalBinding[Node, CodecType, Effect, Context] =
    PositionalBinding(client, methodName)

case class PositionalBinding[Node, CodecType <: Codec[Node], Effect[_], Context](
  client: ClientMeta[Node, CodecType, Effect, Context, ?],
  methodName: String
) extends Binding[Node, CodecType, Effect, Context]:

  inline def call[R](arguments: Tuple)(using context: Context): Effect[R] =
    client.callByPosition[R](methodName)(arguments)(using context)

  inline def notify[R](arguments: Tuple)(using context: Context): Effect[Unit] =
    client.notifyByPosition(methodName)(arguments)(using context)

  inline def named: UnnamedBinding[Node, CodecType, Effect, Context] =
    UnnamedBinding(client, methodName)
