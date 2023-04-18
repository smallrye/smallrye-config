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

The `io.smallrye.config.ConfigSourceInterceptor` is able to intercept the resolution of a configuration name with the 
method `ConfigValue getValue(ConfigSourceInterceptorContext context, String name)`. The `ConfigSourceInterceptorContext`
is used to proceed with the interceptor chain. The chain can be short-circuited by returning an instance of 
`io.smallrye.config.ConfigValue`. The `ConfigValue` objects hold information about the key name, value, config source 
origin and ordinal.

!!! info

    The interceptor chain is applied before any conversion is performed on the configuration value.

```java
package org.acme.config;

import static io.smallrye.config.SecretKeys.doLocked;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigLogging;

@Priority(Priorities.LIBRARY + 200)
public class LoggingConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 367246512037404779L;

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = doLocked(() -> context.proceed(name));
        if (configValue != null) {
            ConfigLogging.log.lookup(configValue.getName(), configValue.getLocation(), configValue.getValue());
        } else {
            ConfigLogging.log.notFound(name);
        }
        return configValue;
    }
}
```

And registration in:

```properties title="META-INF/services/io.smallrye.config.ConfigSourceInterceptor"
org.acme.config.LoggingConfigSourceInterceptor
```

The `LoggingConfigSourceInterceptor` logs looks up configuration names in the provided logging platform. The log 
information includes config name and value, the config source origin and location if exists.

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
