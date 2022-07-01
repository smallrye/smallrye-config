# HOCON Config Source

This Config Source allows to use the [HOCON](https://github.com/lightbend/config/blob/main/HOCON.md) file format to 
load configuration values. The HOCON Config Source loads the
configuration from the file `META-INF/microprofile-config.conf`. It has a lower ordinal (`50`) than the
`microprofile-config.properties`.

The following dependency is required in the classpath to use the HOCON Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-hocon</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

Expressions defined as `${value}` (unquoted) are resolved internally by the HOCON Config Source as described in the
[HOCON Substitutions](https://github.com/lightbend/config/blob/main/HOCON.md#substitutions) documentation. Quoted 
Expressions defined as `"${value}"` are resolved by [SmallRye Config Property Expressions](../config/expressions.md).

Consider:

**hocon.conf**
```conf
{
   foo: "bar",
   hocon: ${foo},
   config: "${foo}" 
}
```

**application.properties**
```properties
config_ordinal=1000
foo=baz
```

The value of `hocon` is `bar` and the value of `config` is `baz` (if the properties source has a higher ordinal).
