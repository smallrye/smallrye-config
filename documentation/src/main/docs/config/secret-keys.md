# Secret Keys

When configuration properties contain passwords or other kinds of secrets, Smallrye Config can hide them to prevent 
accidental exposure of such values.

**This is no way a replacement for securing secrets.** Proper security mechanisms must still be used to secure 
secrets. However, there is still the basic problem that passwords and secrets are generally encoded simply as strings. 
Secret Keys provides a way to "lock" the configuration so that secrets do not appear unless explicitly enabled.

To mark specific keys as secrets, register an instance of `io.smallrye.config.SecretKeysConfigSourceInterceptor` by 
using the interceptor factory as follows:

```java
public class SecretKeysConfigInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
        return new SecretKeysConfigSourceInterceptor(Set.of("secret"));
    }
}
```

Register the factory so that it can be found at runtime by creating a 
`META-INF/services/io.smallrye.config.ConfigSourceInterceptorFactory` file that contains the fully qualified name of 
this factory class.

From this point forward, every lookup to the configuration name `secret` will throw a `SecurityException`.

Access the Secret Keys using the APIs `io.smallrye.config.SecretKeys#doUnlocked(java.lang.Runnable)` 
and `io.smallrye.config.SecretKeys#doUnlocked(java.util.function.Supplier<T>)`.

```java
String secretValue = SecretKeys.doUnlocked(() -> {
    config.getValue("secret", String.class);
});
```

Secret Keys are only unlocked in the context of `doUnlocked`. Once the execution completes, the secrets become locked 
again.
