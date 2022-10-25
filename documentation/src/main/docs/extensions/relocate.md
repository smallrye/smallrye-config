# Relocate

The `io.smallrye.config.RelocateConfigSourceInterceptor` allows to relocate a configuration name to another name, by 
providing a transformation function or just a simple key value map.

When a configuration key is renamed, lookup needs to happen on the new name, but also on the old name if the
config sources are not updated yet. The relocation function gives priority to the new resolved configuration name or
resolves to the old name if no value is found under the new relocation name.

```java
package org.acme;

import java.util.function.Function;
import io.smallrye.config.RelocateConfigSourceInterceptor;

public class MicroProfileConfigRelocateInterceptor extends RelocateConfigSourceInterceptor {
    public MicroProfileConfigRelocateInterceptor(final Function<String, String> mapping) {
        super(name -> name.startsWith("microprofile.config") ?
                      name.replaceAll("microprofile\\.config", "smallrye.config") :
                      name);
    }
}
```

And registration in:
_META-INF/services/io.smallrye.config.ConfigSourceInterceptor_
```properties
io.smallrye.config.MicroProfileConfigRelocateInterceptor
```

The `MicroProfileConfigRelocateInterceptor` can relocate configuration names in the `microprofile.config` namespace
to the `smallrye.config` namespace.
