package automorph.codec.json

import argonaut.Argonaut.jString
import argonaut.{Argonaut, CodecJson, Json}
import automorph.description.OpenRpc
import automorph.description.openrpc._

/** JSON-RPC protocol support for uPickle message codec plugin using JSON format. */
private[automorph] object ArgonautOpenRpc {

  def openRpcCodecJson: CodecJson[OpenRpc] = CodecJson(
    a =>
      Json.obj(
        "openrpc" -> jString(a.openrpc),
        "info" -> Json.obj(
          "title" -> jString(a.info.title),
          "version" -> jString(a.info.version)
        )
      ),
    { c =>
      val info = c.downField("info")
      for {
        openrpc <- c.downField("openrpc").as[String]
        title <- info.downField("title").as[String]
        version <- info.downField("version").as[String]
      } yield OpenRpc(openrpc = openrpc, info = Info(title = title, version = version))
    }
  )
}
