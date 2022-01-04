# Config Source Injection

The Config Source Injection extension allows you to use CDI injection to inject a ConfigSource by name in your CDI 
aware beans, or by looking it up programatically in the CDI `BeanManager`.

## Usage

To use the Config Source Injection, add the following to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-injection</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

### Injecting Sources

You can inject a `ConfigSource` by referencing it by name:

```java
@Inject
@Name("MemoryConfigSource")
private ConfigSource memoryConfigSource;

@Inject
@Name("SysPropConfigSource")
private ConfigSource systemPropertiesConfigSource;
```

You can also get a Map of all config sources. The map key holds the `ConfigSource` name and the map value the 
`ConfigSource`:

```java
@Inject
@ConfigSourceMap
private Map<String,ConfigSource> configSourceMap;
```
