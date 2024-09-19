# Fallback

The `io.smallrye.config.FallbackConfigSourceInterceptor` allows to fall back to another configuration name, by 
providing a transformation function or just a simple key value map.

When a configuration name does not exist, there might be another configuration name that the config can
fall back to provide the same expected behavior. The fallback function is only applied if the original resolved
configuration name is not found and resolved to the fallback name.

```java
package org.acme.config;

import java.util.function.Function;
import io.smallrye.config.FallbackConfigSourceInterceptor;

public class MicroProfileConfigFallbackInterceptor extends FallbackConfigSourceInterceptor {
    public MicroProfileConfigFallbackInterceptor() {
        super(name -> name.startsWith("mp.config") ?
                      name.replaceAll("mp\\.config", "smallrye.config") :
                      name);
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.ConfigSourceInterceptor"
org.acme.config.MicroProfileConfigFallbackInterceptor
```

The `MicroProfileConfigFallbackInterceptor` can fallback configuration names in the `mp.config` namespace
to the `smallrye.config` namespace.

!!! example

    ```properties title="application.properties"
    mp.config.profile=test
    smallrye.config.profile=prod
    ```
    
    A lookup to `mp.config.profile` returns the value `test`.

    ```properties title="application.properties"
    smallrye.config.profile=prod
    ```

    A lookup to `mp.config.profile` returns the value `prod`. The config is not able to find a value for 
    `mp.config.profile`, so the interceptor fallbacks and lookups the value of `smallrye.config.profile`.
