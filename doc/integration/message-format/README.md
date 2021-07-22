# Message format

Structured [message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html) serialization/deserialization plugins.

The underlying format must support storing arbitrarily nested structures of basic data types.

| Class | Artifact | Library | Node Type | Format |
| ---- | --- | --- | --- | --- |
| [CirceJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-circe_2.13/latest/automorph/format/json/CirceJsonFormat.html) (Default) | [automorph-circe](https://mvnrepository.com/artifact/io.automorph/automorph-circe) | [Circe](https://circe.github.io/circe) |[Json](https://circe.github.io/circe/api/io/circe/Json.html) | [JSON](https://www.json.org/) |
| [UpickleJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/format/json/UpickleJsonFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Value](http://com-lihaoyi.github.io/upickle/#uJson) | [JSON](https://www.json.org/) |
| [UpickleMessagePackFormat](https://www.javadoc.io/doc/io.automorph/automorph-upickle_2.13/latest/automorph/format/messagepack/UpickleMessagePackFormat.html) | [automorph-upickle](https://mvnrepository.com/artifact/io.automorph/automorph-upickle) | [uPickle](https://github.com/com-lihaoyi/upickle) |[Msg](http://com-lihaoyi.github.io/upickle/#uPack) | [MessagePack](https://msgpack.org/) |
| [ArgonautJsonFormat](https://www.javadoc.io/doc/io.automorph/automorph-argonaut_2.13/latest/automorph/format/json/ArgonautJsonFormat.html) | [automorph-argonaut](https://mvnrepository.com/artifact/io.automorph/automorph-argonaut) | [Argonaut](http://argonaut.io/doc/) |[Json](http://argonaut.io/scaladocs/#argonaut.Json) | [JSON](https://www.json.org/) |
