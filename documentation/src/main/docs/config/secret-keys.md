# Secret Keys

## Secret Keys Expressions

In SmallRye Config, a secret configuration may be expressed as `${handler::value}`, where the `handler` is the name of 
a `io.smallrye.config.SecretKeysHandler` to decode or decrypt the `value` separated by a double colon `::`. Consider:

```properties
my.secret=${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}

# the encryption key required to decode the secret. It can be set in any source.
smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key=somearbitrarycrazystringthatdoesnotmatter
```

A lookup to `my.secret` will use the `SecretKeysHandler` name `aes-gcm-nopadding` to decode the value 
`DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg`.

It is possible to create a custom `SecretKeysHandler` and provide different ways to decode or decrypt configuration 
values. 

A custom `SecretKeysHandler` requires an implementation of `io.smallrye.config.SecretKeysHandler` or 
`io.smallrye.config.SecretKeysHandlerFactory`. Each implementation requires registration via the `ServiceLoader` 
mechanism, either in `META-INF/services/io.smallrye.config.SecretKeysHandler` or
`META-INF/services/io.smallrye.config.SecretKeysHandlerFactory` files.

!!!danger

     It is not possible to mix Secret Keys Expressions with Property Expressions.

### Crypto

The `smallrye-config-crypto` artifact contains a few out-of-the-box `SecretKeysHandler`s ready for use. It requires 
the following dependency:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-crypto</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

#### AES/GCM/NoPadding `${aes-gcm-nopadding::...}`

- The encoding length is 128.
- The secret and the encryption key (without padding) must be base 64 encoded.

##### Configuration

| Configuration Property 	| Type 	| Default 	|
|---	|---	|---	|
| `smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key`<br>The encription key to use to decode secrets encoded by the `AES/GCM/NoPadding` algorithm. 	| String 	|  	|

### Jasypt

[Jasypt](http://www.jasypt.org) is a java library which allows the developer to add basic encryption capabilities. It 
requires the following dependency:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-jasypt</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

#### Jasypt ``${jasypt::...}``

##### Configuration

| Configuration Property 	| Type 	| Default 	|
|---	|---	|---	|
| `smallrye.config.secret-handler.jasypt.password`<br>The Jasypt password to use 	| String 	|  	|
| `smallrye.config.secret-handler.jasypt.algorithm`<br>The Jasypt algorithm to use 	| String 	|  	|

## Secret Keys Names

When configuration properties contain passwords or other kinds of secrets, Smallrye Config can hide them to prevent 
accidental exposure of such values.

**This is no way a replacement for securing secrets.** Proper security mechanisms must still be used to secure 
secrets. However, there is still the fundamental problem that passwords and secrets are generally encoded simply as 
strings. Secret Keys provides a way to "lock" the configuration so that secrets do not appear unless explicitly enabled.

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
