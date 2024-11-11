# Config Sources

## Custom `ConfigSource`

It’s possible to create a custom ConfigSource as specified in 
[MicroProfile Config](https://github.com/eclipse/microprofile-config/).

With a Custom `ConfigSource` it is possible to read additional configuration values and add them to the `Config` 
instance in a defined ordinal. This allows overriding values from other sources or falling back to other values.

A custom `ConfigSource` requires an implementation of `org.eclipse.microprofile.config.spi.ConfigSource` or 
`org.eclipse.microprofile.config.spi.ConfigSourceProvider`. Each implementation requires registration via the 
`ServiceLoader` mechanism, either in `META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource` or 
`META-INF/services/org.eclipse.microprofile.config.spi.ConfigSourceProvider` files.

Consider a simple in-memory ConfigSource:

```java
package org.acme.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InMemoryConfigSource implements ConfigSource {
    private static final Map<String, String> configuration = new HashMap<>();

    static {
        configuration.put("my.prop", "1234");
    }

    @Override
    public int getOrdinal() {
        return 350;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return InMemoryConfigSource.class.getSimpleName();
    }
}
```

And registration in:

```properties title="META-INF/services/org.eclipse.microprofile.config.spi.ConfigSource"
org.acme.config.InMemoryConfigSource
```

The `InMemoryConfigSource` will be ordered between the `System properties` config source, and the 
`Environment variables` config source due to the `350` ordinal.

In this case, `my.prop` from `InMemoryConfigSource` will only be used if the `Config` instance is unable to find a 
value in `System Properties`, ignoring all the other lower ordinal config sources.

## `ConfigSourceFactory`

Another way to create a `ConfigSource` is via the SmallRye Config `io.smallrye.config.ConfigSourceFactory` API. The 
difference between the SmallRye Config factory and the standard way to create a `ConfigSource` as specified in
[MicroProfile Config](https://github.com/eclipse/microprofile-config/), is the factory ability to provide a context 
with access to the current configuration.

Each implementation of `io.smallrye.config.ConfigSourceFactory` requires registration via the `ServiceLoader` mechanism 
in the `META-INF/services/io.smallrye.config.ConfigSourceFactory` file.

```java
package org.acme.config;

import java.util.Collections;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.PropertiesConfigSource;

public class URLConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue("config.url");
        if (value == null || value.getValue() == null) {
            return Collections.emptyList();
        }

        try {
            return Collections.singletonList(new PropertiesConfigSource(new URL(value.getValue())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(290);
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.ConfigSourceFactory"
org.acme.config.URLConfigSourceFactory
```

By implementing `io.smallrye.config.ConfigSourceFactory`, a list of `ConfigSource` may be provided via the 
`Iterable<ConfigSource> getConfigSources(ConfigSourceContext context)` method. The `ConfigSourceFactory` may also 
assign a priority by overriding the method `OptionalInt getPriority()`. The priority value is used to sort multiple 
`io.smallrye.config.ConfigSourceFactory` (if found). Higher the value, higher the priority.

!!!warning

    The `io.smallrye.config.ConfigSourceFactory` priority does not affect the `ConfigSource` ordinal. These are sorted 
    independently.

When the Factory is initializing, the provided `ConfigSourceContext` may call the method 
`ConfigValue getValue(String name)`. This method looks up configuration names in all `ConfigSource`s that were already 
initialized by the `Config` instance, including sources with lower ordinals than the ones defined in the 
`ConfigSourceFactory`. This means that a `Config` instance is initialized in two steps: first all `ConfigSource` and 
`ConfigSourceProvider` and then the `ConfigSourceFactory`. Only configuation values found in the first step are avaible
to the `ConfigSourceFactory. 

The `ConfigSource` list provided by a `ConfigSourceFactory` is not taken into consideration to 
configure other sources produced by a lower priority `ConfigSourceFactory`.

## Override `ConfigSource` ordinal

The special configuration property name `config_ordinal` can be set in any `ConfigSource` to override its default 
ordinal.

For instance, setting the system property `-Dconfig_ordinal=200` will override the ordinal for the `System properties` 
config source and move it to be looked up after the `Environment Variables` config source.

## Properties

The [PropertiesConfigSource](https://github.com/smallrye/smallrye-config/blob/main/implementation/src/main/java/io/smallrye/config/PropertiesConfigSource.java) 
creates a `ConfigSource` from Java `Properties`, `Map<String, String>` objects or a `.properties` file (referenced by
its URL).

## `.env`

The [DotEnvConfigSourceProvider](https://github.com/smallrye/smallrye-config/blob/main/implementation/src/main/java/io/smallrye/config/DotEnvConfigSourceProvider.java) create a `ConfigSource` from a `.env` file.
