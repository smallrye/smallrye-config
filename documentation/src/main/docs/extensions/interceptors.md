# Interceptors

SmallRye Config provides an interceptor chain that hooks into the configuration values resolution. This is useful to 
implement features like [Profiles](../config/profiles.md) , [Property Expressions](../config/expressions.md), or just 
logging to find out where the config value was loaded from.

An interceptor can be created by implementing the 
[ConfigSourceInterceptor](https://github.com/smallrye/smallrye-config/blob/main/implementation/src/main/java/io/smallrye/config/ConfigSourceInterceptor.java)
interface.

An interceptor requires an implementation of `io.smallrye.config.ConfigSourceInterceptor`. Each implementation requires 
registration via the `ServiceLoader` mechanism in the `META-INF/services/io.smallrye.config.ConfigSourceInterceptor` 
file. Alternatively, interceptors may be registered via the Programmatic API in 
`SmallRyeConfigBuilder#withInterceptors`.

`io.smallrye.config.ConfigSourceInterceptor` exposes two interception points:

- `ConfigValue getValue(ConfigSourceInterceptorContext context, String name)` — intercepts the resolution of a
  configuration value by name.
- `Iterator<String> iterateNames(ConfigSourceInterceptorContext context)` — intercepts the iteration of all known
  configuration property names.

The `ConfigSourceInterceptorContext` is used to proceed with the interceptor chain. Calling `context.proceed(name)`
continues to the next interceptor in the chain. The chain can be short-circuited by returning a custom instance of
`io.smallrye.config.ConfigValue` from `getValue`, or a custom `Iterator<String>` from `iterateNames`. The `ConfigValue`
objects hold information about the key name, value, config source origin and ordinal.

!!! info

    The interceptor chain is applied before any conversion is performed on the configuration value.

```java
package org.acme.config;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.Priorities;

@Priority(Priorities.APPLICATION)
public class LoggingConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        if (configValue != null) {
            System.out.println("Looked up: " + configValue.getName() + "=" + configValue.getValue());
        } else {
            System.out.println("Not found: " + name);
        }
        return configValue;
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.ConfigSourceInterceptor"
org.acme.config.LoggingConfigSourceInterceptor
```

The `LoggingConfigSourceInterceptor` logs each configuration name lookup. The logged information includes the config
name and value, and (from the resolved `ConfigValue`) the config source origin and location if they exist.

## `iterateNames`

The `iterateNames` method intercepts the set of configuration property names visible to the rest of the chain. The
default implementation simply delegates to `context.iterateNames()`. Override it to add, remove, or transform names.

A common use case is hiding sensitive property names from enumeration while still allowing their values to be
retrieved:

```java
package org.acme.config;

import java.util.Iterator;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.Priorities;

@Priority(Priorities.APPLICATION)
public class HidingConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        Iterator<String> names = context.iterateNames();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return names.hasNext();
            }

            @Override
            public String next() {
                String name = names.next();
                // filter out any name starting with "secret."
                return name.startsWith("secret.") ? next() : name;
            }
        };
    }
}
```

!!! info

    `iterateNames` affects the results of `Config#getPropertyNames()` and is used by Config Mappings to discover
    `Map` keys. Interceptors that hide names will also prevent those names from being mapped into `Map` entries.

## `ConfigSourceInterceptorContext`

`ConfigSourceInterceptorContext` provides two ways to continue the chain:

- `proceed(String name)` — passes the lookup to the **next** interceptor in the chain. This is the standard method
  to use in most interceptors.
- `restart(String name)` — re-invokes the **first** interceptor in the chain from the beginning. This is intended for
  relocating or compatibility interceptors that resolve a new name and need the full chain to re-evaluate it, including
  profile expansion and expression resolution. Passing the original name to `restart` can cause an infinite loop, so
  care must be taken.

Example — an interceptor that resolves an alias name and restarts the full chain for the resolved name:

```java
package org.acme.config;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

@Priority(Priorities.APPLICATION)
public class AliasConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (name.equals("app.host")) {
            // Restart the chain with the canonical name so profiles and
            // expressions are re-applied to the resolved name.
            return context.restart("server.host");
        }
        return context.proceed(name);
    }
}
```

Interceptors may also be created with an implementation of `io.smallrye.config.ConfigSourceInterceptorFactory`. Each
implementation requires registration via the `ServiceLoader` mechanism in the
`META-INF/services/io.smallrye.config.ConfigSourceInterceptorFactory` file. Alternatively, interceptors factories may
be registered via the Programmatic API in `SmallRyeConfigBuilder#withInterceptorFactories`.

The `ConfigSourceInterceptorFactory` can initialize an interceptor with access to the current chain
(so it can be used to configure the interceptor and retrieve configuration values) and set the priority.

A `ConfigSourceInterceptor` implementation class can specify a priority by way of the standard
`jakarta.annotation.Priority` annotation. If no priority is explicitly assigned, the default priority value of
`io.smallrye.config.Priorities.APPLICATION` is assumed. If multiple interceptors are registered with the same priority,
then their execution order may be non-deterministic.

A collection of built-in priority constants can be found in `io.smallrye.config.Priorities`. It is recommended to
use `io.smallrye.config.Priorities.APPLICATION` as a baseline for user defined interceptors.
