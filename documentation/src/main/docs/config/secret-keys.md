# Secret Keys

When configuration properties contain passwords or other kinds of secrets, Smallrye Config can hide them to prevent 
accidental exposure of such values.

**This is no way a replacement for securing secrets.** Proper security mechanisms must still be used to secure 
secrets. However, there is still the basic problem that passwords and secrets are generally encoded simply as strings. 
Secret Keys provides a way to "lock" the configuration so that secrets do not appear unless explicitly enabled.

Secret Keys requires the list of the configuration property names that must be hidden. This can be supplied 
in `SmallRyeConfigBuilder#withSecretKeys`.

```java
SmallRyeConfig config = new SmallRyeConfigBuilder()
    .addDefaultSources()
    .addDefaultInterceptors()
    .withSources(KeyValuesConfigSource.config(keyValues))
    .withSecretKeys("secret")
    .build()
```

From this point forward, every lookup to the configuration name `secret` will throw a `SecurityException`.

Access to the Secret Keys, is available via the APIs `io.smallrye.config.SecretKeys#doUnlocked(java.lang.Runnable)` 
and `io.smallrye.config.SecretKeys#doUnlocked(java.util.function.Supplier<T>)`.

```java
String secretValue = SecretKeys.doUnlocked(() -> {
    config.getValue("secret", String.class);
});
```

Secret Keyes are only unlocked in the context of `doUnlocked`. Once the execution completes, the secrets become locked 
again.
