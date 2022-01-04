# Property Expressions

SmallRye Config provides property expressions expansion on configuration values. An expression string is a mix of plain
strings and expression segments, which are wrapped by the sequence ${ ... }.

For instance, the following configuration properties file:

```properties
remote.host=smallrye.io
callable.url=https://${remote.host}/
```

The resolved value of the `callable.url` property is `https://smallrye.io/`.

Additionally, the Expression Expansion engine supports the following segments:

- `${expression:value}` - Provides a default value after the `:` if the expansion doesn’t find a value.
- `${my.prop${compose}}` - Composed expressions. Inner expressions are resolved first.
- `${my.prop}${my.prop}` - Multiple expressions.

If an expression cannot be expanded and no default is supplied a `NoSuchElementException` is thrown.

Expression expansion may be selectively disabled with `io.smallrye.config.Expressions`:

```java
Config config = ConfigProvider.getConfig();
String url = Expressions.withoutExpansion(() -> config.getValue("callable.url", String.class));
```
