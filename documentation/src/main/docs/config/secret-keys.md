# Secret Keys

## Secret Keys Expressions

In SmallRye Config, a secret configuration may be expressed as `${handler::value}`, where the `handler` is the name of 
a `io.smallrye.config.SecretKeysHandler` to decode or decrypt the `value` separated by a double colon `::`.

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

!!! example

    ```properties title="application.properties"
    smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key=somearbitrarycrazystringthatdoesnotmatter

    my.secret=${aes-gcm-nopadding::DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg}
    ``` 

    The `${aes-gcm-nopadding::...}` `SecretKeyHandler` requires 
    `smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key` configuration to state the encryption key to be 
    used by the `aes-gcm-nopaddin` handler.

    A lookup to `my.secret` will use the `SecretKeysHandler` name `aes-gcm-nopadding` to decode the value 
    `DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg`.

!!! info

    It is possible to generate the encrypted secret with the following [JBang](http://jbang.dev/) script:

    ```shell
    jbang https://raw.githubusercontent.com/smallrye/smallrye-config/main/documentation/src/main/docs/config/secret-handlers/encryptor.java -s=<secret> -k=<encryptionKey>`
    ```

##### Configuration

| Configuration Property 	| Type 	| Default 	|
|---	|---	|---	|
| `smallrye.config.secret-handler.aes-gcm-nopadding.encryption-key`<br>The encription key to use to decode secrets encoded by the `AES/GCM/NoPadding` algorithm. 	| String 	|  	|

### Jasypt

[Jasypt](http://www.jasypt.org) is a java library which allows the developer to add basic encryption capabilities. Add 
the following dependency in your project to use it:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-jasypt</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

#### Jasypt `${jasypt::...}`

!!! example

    ```properties title="application.properties"
    smallrye.config.secret-handler.jasypt.password=jasypt
    smallrye.config.secret-handler.jasypt.algorithm=PBEWithHMACSHA512AndAES_256

    my.secret=${jasypt::ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)}
    ```
    The `${jasypt::...}` `SecretKeyHandler` requires both `smallrye.config.secret-handler.jasypt.password` and 
    `smallrye.config.secret-handler.jasypt.algorithm` configurations to state the password and the algorithm to be
    used by the Jasypt encryptor.

    Jasypt encrypted values must be set with the handler expression as `${jasypt::ENC(value)}`. Note that the 
    encrypted value must be generated using the proper Jasypt encryptor with the same password and algorithm set in 
    the confguration.

    A possible encrypted value for `12345678` is `ENC(wqp8zDeiCQ5JaFvwDtoAcr2WMLdlD0rjwvo8Rh0thG5qyTQVGxwJjBIiW26y0dtU)`

    Lookups to the configuration `my.secret` will automatically decrypt the value with Jasypt and provide the original
    `12345678` string.

!!! info

    It is possible to generate the encrypted secret with the following [JBang](http://jbang.dev/) script:
    
    ```shell  
    jbang https://raw.githubusercontent.com/smallrye/smallrye-config/main/documentation/src/main/docs/config/secret-handlers/jasypt.java -s=<secret> -p=<password>
    ```

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
