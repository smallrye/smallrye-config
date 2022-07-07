# JSON Converter

The JSON Converter allows to convert a configuration value into a `JsonObject` or `JsonArray`.

The following dependency is required in the classpath to use the JSON Converter:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-converter-json</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

`JsonObject` or `JsonArray` are automatically converted from their `String` value.

```java
@Inject
@ConfigProperty(name = "someJsonArray")
private JsonArray someValue;

@Inject
@ConfigProperty(name = "someJsonObject")
private JsonObject someValue;
```

```properties
someJsonArray=["value1","value2","value3"]
someJsonObject={"foo": "bar", "count":100}
```

The value of the configuration properties must be valid JSON, or a `JsonParsingException` is thrown.
