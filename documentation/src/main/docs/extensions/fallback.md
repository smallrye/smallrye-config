# Fallback

The `io.smallrye.config.FallbackConfigSourceInterceptor` allows to fall back to another configuration name, by 
providing a transformation function or just a simple key value map.

When a configuration name does not exist, there might be another configuration name that the config can
fall back to provide the same expected behavior. The fallback function is only applied if the original resolved
configuration name is not found and resolved to the fallback name.

```java
package org.acme;

import java.util.function.Function;
import io.smallrye.config.FallbackConfigSourceInterceptor;

public class MicroProfileConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {
    public MicroProfileConfigFallbackInterceptor(final Function<String, String> mapping) {
        super(name -> name.startsWith("microprofile.config") ?
                      name.replaceAll("microprofile\\.config", "smallrye.config") :
                      name);
    }
}
```

And registration in:
_META-INF/services/io.smallrye.config.ConfigSourceInterceptor_
```properties
io.smallrye.config.MicroProfileConfigFallbackInterceptor
```

The `MicroProfileConfigFallbackInterceptor` can fallback configuration names in the `microprofile.config` namespace
to the `smallrye.config` namespace.
