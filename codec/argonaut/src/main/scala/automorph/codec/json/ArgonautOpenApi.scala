package automorph.codec.json

import argonaut.Argonaut.jString
import argonaut.{Argonaut, CodecJson, Json}
import automorph.description.OpenApi
import automorph.description.openapi._

/** JSON-RPC protocol support for uPickle message codec plugin using JSON format. */
private[automorph] object ArgonautOpenApi {

  def openApiCodecJson: CodecJson[OpenApi] = CodecJson(
    a =>
      Json.obj(
        "openapi" -> jString(a.openapi),
        "info" -> Json.obj(
          "title" -> jString(a.info.title),
          "version" -> jString(a.info.version)
        )
      ),
    { c =>
      val info = c.downField("info")
      for {
        openapi <- c.downField("openapi").as[String]
        title <- info.downField("title").as[String]
        version <- info.downField("version").as[String]
      } yield OpenApi(openapi = openapi, info = Info(title = title, version = version))
    }
  )
}
