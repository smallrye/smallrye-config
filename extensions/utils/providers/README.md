[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Configsource providers

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-providers/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-providers)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-providers.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-providers)

Util library that makes all Configsources available via CDI

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configsource-providers</artifactId>
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
