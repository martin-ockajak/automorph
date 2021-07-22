# Effect system

@:navigationTree {
  entries = [ ]
}

Computational [effect system](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html) plugins.

The underlying runtime must support monadic composition of effectful values.

| Class | Artifact | Library | Effect Type |
| ---- | --- | --- | --- |
| [FutureSystem](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/FutureSystem.html) (Default) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/core/futures.html) | [Future](https://www.scala-lang.org/api/current/scala/concurrent/Future.html) |
| [TrySystem](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/TrySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html) | [Try](https://www.scala-lang.org/api/2.13.6/scala/util/Try.html) |
| [IdentitySystem](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem.html) | [automorph-standard](https://mvnrepository.com/artifact/io.automorph/automorph-standard) | [Standard](https://www.scala-lang.org/) | [Identity](https://www.javadoc.io/doc/io.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html) |
| [ZioSystem](https://www.javadoc.io/doc/io.automorph/automorph-zio_2.13/latest/automorph/system/ZioSystem.html) | [automorph-zio](https://mvnrepository.com/artifact/io.automorph/automorph-zio) | [ZIO](https://zio.dev/) | [RIO](https://javadoc.io/doc/dev.zio/zio_2.13/latest/zio/RIO$.html) |
| [MonixSystem](https://www.javadoc.io/doc/io.automorph/automorph-monix_2.13/latest/automorph/system/MonixSystem.html) | [automorph-monix](https://mvnrepository.com/artifact/io.automorph/automorph-monix) | [Monix](https://monix.io/) | [Task](https://monix.io/api/current/monix/eval/Task.html) |
| [CatsEffectSystem](https://www.javadoc.io/doc/io.automorph/automorph-cats-effect_2.13/latest/automorph/system/CatsEffectSystem.html) | [automorph-cats-effect](https://mvnrepository.com/artifact/io.automorph/automorph-cats-effect) | [Cats Effect](https://typelevel.org/cats-effect/) | [IO](https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html) |
| [ScalazSystem](https://www.javadoc.io/doc/io.automorph/automorph-scalaz_2.13/latest/automorph/system/ScalazSystem.html) | [automorph-scalaz](https://mvnrepository.com/artifact/io.automorph/automorph-scalaz) | [Scalaz](https://github.com/scalaz) | [IO](https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html) |
