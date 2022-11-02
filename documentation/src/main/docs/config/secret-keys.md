# Secret Keys

When configuration properties contain passwords or other kinds of secrets, Smallrye Config can hide them to prevent 
accidental exposure of such values.

**This is no way a replacement for securing secrets.** Proper security mechanisms must still be used to secure 
secrets. However, there is still the basic problem that passwords and secrets are generally encoded simply as strings. 
Secret Keys provides a way to "lock" the configuration so that secrets do not appear unless explicitly enabled.

To mark specific keys as secret, register an instance of `io.smallrye.config.SecretKeysConfigSourceInterceptor`
using your own interceptor factory, like this:

```java
public class SecretKeysConfigInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(ConfigSourceInterceptorContext context) {
        return new SecretKeysConfigSourceInterceptor(Set.of("secret"));
    }
}
```

You will need to register this factory to be found at runtime by creating a `META-INF/services/io.smallrye.config.ConfigSourceInterceptorFactory`
file containing the fully qualified name of this factory class.

From this point forward, every lookup to the configuration name `secret` will throw a `SecurityException`.

Access to the Secret Keys, is available via the APIs `io.smallrye.config.SecretKeys#doUnlocked(java.lang.Runnable)` 
and `io.smallrye.config.SecretKeys#doUnlocked(java.util.function.Supplier<T>)`.

```java
String secretValue = SecretKeys.doUnlocked(() -> {
    config.getValue("secret", String.class);
});
```

Secret Keys are only unlocked in the context of `doUnlocked`. Once the execution completes, the secrets become locked 
again.
