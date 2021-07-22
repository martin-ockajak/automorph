# Handler

The handler provides automatic creation of remote RPC endpoint bindings for existing API instances and subsequent processing RPC requests.

Depends on:

* [Effect system](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html)
* [Message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html)

```
   .--------------------------.     .---------.
   | Server message transport | --> | Handler |
   '--------------------------'     '---------'
                        |            |      |
                        v            v      v
                      .---------------.    .----------------.
                      | Effect system |    | Message format |
                      '---------------'    '----------------'
```

