# Architecture

## [Client](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/Client.html)

The client provides automatic creation of transparent proxy instances for remote RPC endpoints defined by existing API classes. Additionally, it also
supports direct calls and notifications of remote API methods.

Depends on:

* [Effect system](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/EffectSystem.html)
* [Message format](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/MessageFormat.html)
* [Client message transport](https://www.javadoc.io/doc/io.automorph/automorph-spi_2.13/latest/automorph/spi/ClientMessageTransport.html)

```
                  .--------.     .--------------------------.
                  | Client | --> | Client message transport |
                  '--------'     '--------------------------'
                   |     |             |
                   v     v             v
   .----------------.   .---------------.
   | Message format |   | Effect system |
   '----------------'   '---------------'
```

## Handler

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

