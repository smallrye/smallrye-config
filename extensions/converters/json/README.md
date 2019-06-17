[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Json converter

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configconverter-json/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configconverter-json)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configconverter-json.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configconverter-json)

This extension allows you to inject a JsonObject or JsonArray as a ConfigProperty

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configconverter-json</artifactId>
        <version>XXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Example

In the java code:

```java

    @Inject
    @ConfigProperty(name = "someJsonArray")
    private JsonArray someValue;

    @Inject
    @ConfigProperty(name = "someJsonObject")
    private JsonObject someValue;

```

When using the property:

```

    someJsonArray=["value1","value2","value3"]
    someJsonObject={"foo": "bar", "count":100}

```    
