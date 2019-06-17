# Json converter

This extension allows you to inject a JsonObject or JsonArray as a ConfigProperty

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.ext</groupId>
        <artifactId>configconverters-json</artifactId>
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
