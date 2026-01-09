# Custom `Converter`

It is possible to create a custom `Converter` type as specified in 
[MicroProfile Config](https://github.com/eclipse/microprofile-config/).

A custom `Converter` requires an implementation of `org.eclipse.microprofile.config.spi.Converter`. Each implementation 
requires registration via the `ServiceLoader` mechanism in the 
`META-INF/services/org.eclipse.microprofile.config.spi.Converter` file. Consider:

```java
package org.acme.config;

public class CustomValue {

    private final int number;

    public CustomValue(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
```

The corresponding converter can look like:

```java
package org.acme.config;

import org.eclipse.microprofile.config.spi.Converter;

public class CustomValueConverter implements Converter<CustomValue> {

    @Override
    public CustomValue convert(String value) {
        return new CustomValue(Integer.parseInt(value));
    }
}
```

And registration in:

```properties title="META-INF/services/org.eclipse.microprofile.config.spi.Converter"
org.acme.config.CustomValue
```

!!! warning

    The custom `Converter` class must be `public`, must have a `public` constructor with no arguments, and must not be 
    abstract.

The `CustomValueConverter` converts the configuration value to the `CustomValue` type automatically.

```java
Config config = Config.getOrCreate();
CustomValue value = config.getValue("custom.value", CustomValue.class);
```

The `jakarta.annotation.Priority` annotation overrides the `Converter` priority and change converters precedence to fine 
tune the execution order. By default, if no `@Priority` is specified by the `Converter`, the converter is registered 
with a priority of `100`. Consider:

```java
package org.acme.config;

import jakarta.annotation.Priority;
import org.eclipse.microprofile.config.spi.Converter;

@Priority(150)
public class SecretConverter implements Converter<CustomValue> {

    @Override
    public CustomValue convert(String value) {
        final int secretNumber;
        if (value.startsFrom("OBF:")) {
            secretNumber = Integer.parseInt(SecretDecoder.decode(value));
        } else {
            secretNumber = Integer.parseInt(value);
        }

        return new CustomValue(secretNumber);
    }
}
```

Two `Converter`s, (`CustomValueConverter` and `SecretConverter`) can convert the same type `CustomValue`. Since 
`SecretConverter` has a priority of `150`, it will be used instead of a `CustomValueConverter` which has a default 
priority of `100` (no annotation).
