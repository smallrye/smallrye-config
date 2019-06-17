[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# List Converter

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configconverter-list/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configconverter-list)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configconverter-list.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configconverter-list)

This extension allows you to inject a List or Array as a ConfigProperty

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configconverter-list</artifactId>
        <version>XXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Example

In the java code:

```java

    @Inject
    @ConfigProperty(name = "someList")
    private List<String> someValue;

    @Inject
    @ConfigProperty(name = "someArray")
    private String[] someValue;

```

When using the property:

```

    someList=value1,value2,value3
    someArray=value1,value2,value3

```    
