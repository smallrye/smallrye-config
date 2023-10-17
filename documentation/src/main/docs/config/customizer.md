# Customizer

A `SmallRyeConfigBuilderCustomizer` allows to customize a `SmallRyeConfigBuilder`, used to create the `SmallRyeConfig` 
instance.

Registration of a `SmallRyeConfigBuilderCustomizer` is done via the `ServiceLoader` mechanism by providing the
implementation classes in a `META-INF/services/io.smallrye.config.SmallRyeConfigBuilderCustomizer` file. Alternatively, 
customizers may be registered via the Programmatic API in `SmallRyeConfigBuilder#withCustomizers`.

The `SmallRyeConfigBuilderCustomizer` may also
assign a priority by overriding the default method `int priority()`. Customizers are sorted by ascending priority and 
executed in that order, meaning that higher numeric priorities will execute last, possible overriding values set by 
previous customizers.

```java title="CustomConfigBuilder"
package org.acme.config;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class CustomConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("my.default", "1234");
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.SmallRyeConfigBuilderCustomizer"
org.acme.config.CustomConfigBuilder
```

The `CustomConfigBuilder` will be executed when creating a new `SmallRyeConfig` from a `SmallRyeConfigBuilder`:

```java
SmallRyeConfig config = new SmallRyeConfigBuilder().build();
config.getValue("my.default", int.class);
```

A look up to `my.default` returns the value `1234`, registered by the `CustomConfigBuilder`.

