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

## Escaping

A `$` that begins an expression segment can be escaped with a backslash to treat it as a literal character:

```properties
# value is: ${my.prop}
expression=\${my.prop}
```

A backslash that immediately precedes a `$` can itself be escaped with another backslash:

```properties
# my.prop=value
# value is: \value
expression=\\${my.prop}
```

Escaping only applies to values that contain an expression segment. In a value that has no `${ ... }` segment,
every backslash is treated as a literal character and nothing needs escaping (for example, a Windows path such as
`C:\some\path` is used as-is).

As soon as a value contains an expression segment, the whole value is processed for escapes and backslash becomes an
escape character everywhere in it, not just before a `$`. Standard escape sequences are interpreted (`\n`, `\r`, `\t`,
`\b`, `\f`) and a backslash before any other character is dropped. To keep a literal backslash in such a value, double
it:

```properties
# value is: C:\some\path\value (my.prop=value)
expression=C:\\some\\path\\${my.prop}
```

## Disabling Expansion

Expression expansion may be selectively disabled for a block of code using `io.smallrye.config.Expressions.withoutExpansion()`.
The raw (unexpanded) value is returned for any property looked up within the block:

```properties
callable.url=https://${remote.host}/
```

```java
Config config = Config.getOrCreate();
// returns "https://${remote.host}/"
String url = Expressions.withoutExpansion(() -> config.getValue("callable.url", String.class));
```
