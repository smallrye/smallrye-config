# Relocate

The `io.smallrye.config.RelocateConfigSourceInterceptor` allows to relocate a configuration name to another name, by 
providing a transformation function or just a simple key value map.

When a configuration key is renamed, lookup needs to happen on the new name, but also on the old name if the
config sources are not updated yet. The relocation function gives priority to the new resolved configuration name or
resolves to the old name if no value is found under the new relocation name.

```java
package org.acme.config;

import java.util.function.Function;
import io.smallrye.config.RelocateConfigSourceInterceptor;

public class MicroProfileConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {
    public MicroProfileConfigRelocateInterceptor(final Function<String, String> mapping) {
        super(name -> name.startsWith("mp.config") ?
                      name.replaceAll("mp\\.config", "smallrye.config") :
                      name);
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.ConfigSourceInterceptor"
org.acme.config.MicroProfileConfigRelocateInterceptor
```

The `MicroProfileConfigRelocateInterceptor` can relocate configuration names in the `mp.config` namespace
to the `smallrye.config` namespace.

!!! example

    ```properties title="application.properties"
    mp.config.profile=test
    smallrye.config.profile=prod
    ```
    
    A lookup to `mp.config.profile` returns the value `prod`. The config finds a valid value in the relocated name 
    `smallrye.config.profile`, so the interceptor will use this value instead of the one in `mp.config.profile`.
