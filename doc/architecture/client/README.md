# [Client](https://www.javadoc.io/doc/io.automorph/automorph-core_2.13/latest/automorph/Client.html)

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
