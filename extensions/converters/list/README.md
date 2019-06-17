# List Converter

This extension allows you to inject a List or Array as a ConfigProperty

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.ext</groupId>
        <artifactId>configconverters-list</artifactId>
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
