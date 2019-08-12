# Configsource providers

Util library that makes all Config sources available via CDI

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.config</groupId>
        <artifactId>smallrye-config-cdi-provider</artifactId>
        <version>XXXX</version>
    </dependency>

```

## Injecting sources

You can inject a certain ConfigSource by referencing it by name:

```java

    @Inject @Name("MemoryConfigSource")
    private ConfigSource memoryConfigSource;

    @Inject @Name("SysPropConfigSource")
    private ConfigSource systemPropertiesConfigSource;

```

You can also get a Map of all config sources, with the key in the map the name of the source and the value the source:

```java

    @Inject @ConfigSourceMap
    private Map<String,ConfigSource> configSourceMap;

```
