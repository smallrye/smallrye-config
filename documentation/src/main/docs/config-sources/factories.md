# Config Source Factory

Another way to create a `ConfigSource` is via the `ConfigSourceFactory`. The difference between the SmallRye Config 
Factory and the standard way to make a `ConfigSource` as specified in MicroProfile Config is the Factory's ability to 
provide a context with access to the available configuration. With the `ConfigSourceFactory` it is possible to 
bootstrap a `ConfigSource` that configures itself with previously initialized `ConfigSource's`.

By implementing 
[ConfigSourceFactory](https://github.com/smallrye/smallrye-config/blob/main/implementation/src/main/java/io/smallrye/config/ConfigSourceFactory.java), 
a list of `ConfigSource's` may be provided via the `
Iterable<ConfigSource> getConfigSources(ConfigSourceContext context)` method. The `ConfigSourceFactory` may also 
assign a priority by overriding the default method `OptionalInt getPriority()`. The priority only sorts the factories 
during initialization. After initialization, the provided `ConfigSources` will use their own ordinal and sorted with 
all `ConfigSources` available in the `Config` instance.

When the Factory initializes, the provided `ConfigSourceContext` may call the method 
`ConfigValue getValue(String name)`. This method looks up configuration names in all `ConfigSource's` already 
initialized by the `Config` instance, including sources with lower ordinals than the ones defined in the 
`ConfigSourceFactory`. The `ConfigSourceFactory` does not consider `ConfigSources's` provided by other 
`ConfigSourceFactory's` (the priority does not matter).

Registration of a `ConfigSourceFactory` is done via the `ServiceLoader` mechanism by providing the
implementation classes in a `META-INF/services/io.smallrye.config.ConfigSourceFactory` file. Alternatively, factories
may be registered via the Programmatic API in `SmallRyeConfigBuilder#withSources`.

A `ConfigSourceFactory` requires an implementation of `io.smallrye.config.ConfigSourceFactory`. Each implementation 
requires registration via the `ServiceLoader` mechanism in the 
`META-INF/services/io.smallrye.config.ConfigSourceFactory` file. Alternatively, interceptors may be registered via the 
Programmatic API in `SmallRyeConfigBuilder#withSources`.

```java
package org.acme.config

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;

public class FileSystemConfigSourceFactory implements ConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final ConfigValue value = context.getValue("org.acme.config.file.locations");
        if (value == null || value.getValue() == null) {
            return Collections.emptyList();
        }

        try {
            return List.of(new PropertiesConfigSource(toURL(value.getValue()), 250));
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private URL toURL(final String value) {
        try {
            return new URI(value).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw ConfigMessages.msg.uriSyntaxInvalid(e, value);
        }
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.ConfigSourceFactory"
org.acme.config.FileSystemConfigSourceFactory
```

The `FileSystemConfigSourceFactory` look ups the configuration value for `org.acme.config.file.locations`, and uses it 
to set up an additional `ConfigSource`.

Alternatively, a `ConfigurableConfigSourceFactory` accepts a `ConfigMapping` interface to configure the `ConfigSource`:

```java
@ConfigMapping(prefix = "org.acme.config.file")
interface FileSystemConfig {
    List<URL> locations();   
}
```

```java
public class FileSystemConfigurableConfigSourceFactory<FileSystemConfig> implements ConfigurableConfigSourceFactory {
    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context, FileSystemConfig config) {
        
    }
}
```

With a `ConfigurableConfigSourceFactory` it is not required to look up the configuration values with 
`ConfigSourceContext`. The values are automatically mapped with the defined `@ConfigMapping`.
