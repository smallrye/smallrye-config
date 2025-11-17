# Mappings 

With SmallRye Config Mappings, it is possible to group multiple configuration properties in a single interface that 
share the same prefix (or namespace). It supports the following set of features:

- Automatic conversion of the configuration type, including `List`, `Set`, `Map`, `Optional` and primitive types.
- Nested Config Mapping groups.
- Configuration Properties Naming Strategies
- Integration with Bean Validation

## Mapping Rules

A complex object type uses the following rules to map configuration values to their member values:

- A configuration path is built by taking the object type prefix (or namespace) and the mapping member name
- The member name is converted to its kebab-case format
- If the member name is represented as a getter, the member name is taken from its property name equivalent, and then
  converted to its kebab-case format
- The configuration value is automatically converted to the member type
- The configuration path is required to exist with a valid configuration value or the mapping will fail

!!! info

    Kebab-case - the method name is derived by replacing case changes with a dash to map the configuration property.

A Config Mapping requires an interface with minimal metadata configuration annotated with 
`io.smallrye.config.ConfigMapping`:

```java
@ConfigMapping(prefix = "server")
interface Server {
    String host();

    int port();
}
```

The `Server` interface is able to map configurations with the name `server.host` into the `Server#host()` method and 
`server.port` into `Server#port()` method. The configuration property name to lookup is built from the prefix, and the 
method name with `.` (dot) as the separator.

!!! warning

    If a mapping fails to match a configuration property the config system throws a `NoSuchElementException`, unless 
    the mapped element is an `Optional`.

The `@ConfigMapping` interface must obey the following rules:

- A mapping method cannot accept parameters
- A mapping method return type cannot be void
- A mapping cannot use self-reference types
- `default` methods are allowed

## Registration

Registration of Config Mappings is automatic in CDI aware environments with the `@ConfigMapping` annotation. 

In non-CDI environments, the Config Mapping can be registered via `SmallRyeConfigBuilder#withMapping`. In this case, 
the `@ConfigMapping` is completely optional (but recommendeded to set the prefix).

```java
SmallRyeConfig config = new SmallRyeConfigBuilder()
        .withMapping(Server.class)
        .build();
```

## Retrieval

A config mapping interface can be injected into any CDI aware bean:

```java
@ApplicationScoped
class BusinessBean {
    @Inject
    Server server;

    public void businessMethod() {
        String host = server.host();
    }
}
```

In non-CDI environments, use the API `io.smallrye.config.SmallRyeConfig#getConfigMapping` to retrieve the config 
mapping instance:

```java
SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
Server server = config.getConfigMapping(Server.class);
```

!!! info

    Config Mapping instances are cached. They are populated when the `SmallRyeConfig` instance is initialized and 
    their values are not updated on `ConfigSource` changes. 

For a Config Mapping to be valid, it needs to match every configuration property name contained in the `Config` under 
the specified prefix set in `@ConfigMapping`. This prevents unknown configuration properties in the `Config`. This 
behaviour can be disabled with the configuration `smallrye.config.mapping.validate-unknown=false`, or by ignoring 
specified paths with `io.smallrye.config.SmallRyeConfigBuilder.withMappingIgnore`. 

## Defaults

The `io.smallrye.config.WithDefault` annotation allows to set a default property value into a mapping (and prevent
errors if the configuration value is not available in any `ConfigSource`).

```java
public interface Defaults {
    @WithDefault("foo")
    String foo();

    @WithDefault("bar")
    String bar();
}
```

No configuration properties are required. The `Defaults#foo()` will return the value `foo` and `Defaults#bar()` will
return the value `bar`.

## Nested Groups

A nested mapping provides a way to map sub-groups of configuration properties.

- A nested type contributes with its name (converted to its kebab-case format)
- The configuration path is built by taking the root object type prefix (or namespace), the nested type name and the 
member name of the nested type

```java
@ConfigMapping(prefix = "server")
public interface Server {
    String host();

    int port();

    Log log();

    interface Log {
        boolean enabled();

        String suffix();

        boolean rotate();
    }
}
```

```properties
server.host=localhost
server.port=8080
server.log.enabled=true
server.log.suffix=.log
server.log.rotate=false
```

The method name of a mapping group acts as a sub-prefix in the property name. In this case the matching property to
`Server.Log#enabled` is `server.log.enabled`.

## Hierarchy

A config mapping can extend another mapping and inherit all its super members:

```java
public interface Parent {
    String name();
}

@ConfigMapping(prefix = "child")
public interface Child extends Parent {
    
}
```

And override members:

```java
public interface Parent {
    String name();
}

@ConfigMapping(prefix = "child")
public interface Child extends Parent {
    @WithName("child-name")
    String name();
}
```

## Overriding property names

### `@WithName`

If a method name and a property name do not match, the `io.smallrye.config.WithName` annotation can 
override the method name mapping and use the name supplied in the annotation.

```java
@ConfigMapping(prefix = "server")
interface Server {
    @WithName("name")
    String host();

    int port();
}
```

```properties
server.name=localhost
server.port=8080
```

### `@WithParentName`

The `io.smallrye.config.WithParentName` annotation allows configurations mappings to inherit its parent container name, 
simplifying the configuration property name required to match the mapping.

```java
@ConfigMapping(prefix = "server")
interface Server {
    @WithParentName
    ServerHostAndPort hostAndPort();

    @WithParentName
    ServerInfo info();
}

interface ServerHostAndPort {
    String host();

    int port();
}

interface ServerInfo {
    String name();
}
```

```properties
server.host=localhost
server.port=8080
server.name=konoha
```

Without the `@WithParentName` the method `ServerInfo#name` maps the configuration property `server.info.name`. With
`@WithParentName`, the `Server#info` mapping will inherit the parent name from `Server` and `ServerInfo#name` maps to 
the property `server.name` instead.

### NamingStrategy

Method names in camelCase map to kebab-case configuration property names by default.

```java
@ConfigMapping(prefix = "server")
interface Server {
    String theHost();

    int thePort();
}
```

```properties
server.the-host=localhost
server.the-port=8080
```

The mapping strategy can be adjusted by setting `namingStrategy` value in the `@ConfigMapping` annotation.

```java
@ConfigMapping(prefix = "server", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface ServerVerbatimNamingStrategy {
    String theHost();

    int thePort();
}
```

```properties
server.theHost=localhost
server.thePort=8080
```

The `@ConfigMapping` annotation support the following naming stategies:

- KEBAB_CASE - The method name is derived by replacing case changes with a dash to map the configuration property.
- VERBATIM - The method name is used as is to map the configuration property.
- SNAKE_CASE - The method name is derived by replacing case changes with an underscore to map the configuration property.

## Conversion

A config mapping interface support automatic conversions of all types available for conversion in `Config`.

```java
@ConfigMapping
public interface SomeTypes {
    @WithName("int")
    int intPrimitive();

    @WithName("int")
    Integer intWrapper();

    @WithName("long")
    long longPrimitive();

    @WithName("long")
    Long longWrapper();

    @WithName("float")
    float floatPrimitive();

    @WithName("float")
    Float floatWrapper();

    @WithName("double")
    double doublePrimitive();

    @WithName("double")
    Double doubleWrapper();

    @WithName("char")
    char charPrimitive();

    @WithName("char")
    Character charWrapper();

    @WithName("boolean")
    boolean booleanPrimitive();

    @WithName("boolean")
    Boolean booleanWrapper();
}
```

```properties
int=9
long=9999999999
float=99.9
double=99.99
char=c
boolean=true
```

This is also valid for `Optional` and friends.

```java
@ConfigMapping
public interface Optionals {
    Optional<Server> server();

    Optional<String> optional();

    @WithName("optional.int")
    OptionalInt optionalInt();

    interface Server {
        String host();

        int port();
    }
}
```

In this case, the mapping won’t fail if the configuraton properties values are missing.

### `@WithConverter`

The `io.smallrye.config.WithConverter` annotation provides a way to set a specific `Converter` in a mapping.

```java
@ConfigMapping
public interface Converters {
    @WithConverter(FooBarConverter.class)
    String foo();
}

public static class FooBarConverter implements Converter<String> {
    @Override
    public String convert(final String value) {
        return "bar";
    }
}
```

```properties
foo=foo
```

A call to `Converters.foo()` results in the value `bar`.

## Collections

A config mapping is also able to map the collections types `List` and `Set`.

- A member with a `Collection` type requires the configuration name to be in its indexed format
- Each configuration name, plus its index maps the configuration value to the corresponding `Collection` element in the 
object type
- The index must be part of the configuration path, by appending the index between square brackets to the`Collection` 
member
- The index specified in the configuration name is used to order the element in the `Collection`
- Missing elements or gaps are removed

```java
@ConfigMapping(prefix = "server")
public interface ServerCollections {
    Set<Environment> environments();

    interface Environment {
        String name();

        List<App> apps();

        interface App {
            String name();

            List<String> services();

            Optional<List<String>> databases();
        }
    }
}
```

```properties
server.environments[0].name=dev
server.environments[0].apps[0].name=rest
server.environments[0].apps[0].services=bookstore,registration
server.environments[0].apps[0].databases=pg,h2
server.environments[0].apps[1].name=batch
server.environments[0].apps[1].services=stock,warehouse
```

The `List` and `Set` mappings can use [Indexed Properties](indexed-properties.md) to map configuration values in 
mapping groups.

!!! info

    A `List` mapping is backed by an `ArrayList`, and a `Set` mapping is backed by a `HashSet`.

## Maps

A config mapping is also able to map a `Map`.

- A member with a `Map` type requires an additional configuration name added to the configuration path of the `Map`
member to act as a map key
- The additional configuration name maps a Map entry with the configuration name as the `Map` entry key and 
the configuration value as the Map entry value

```java
@ConfigMapping(prefix = "server")
public interface Server {
    String host();

    int port();

    Map<String, String> form();

    Map<String, List<Alias>> aliases();
    
    interface Alias {
        String name();
    }
}
```

```properties
server.host=localhost
server.port=8080
server.form.index=index.html
server.form.login.page=login.html
server.form.error.page=error.html
server.aliases.localhost[0].name=prod
server.aliases.localhost[1].name=127.0.0.1
server.aliases.\"io.smallrye\"[0].name=smallrye
```

The configuration property name needs to specify an additional segment to act as the map key. The `server.form` matches 
the `Server#form` `Map` and the segments `index`, `login.page` and `error.page` represent the `Map` 
keys.

!!! info

    A `Map` mapping is backed by an `HashMap`.

When populating a `Map`, `SmallRyeConfig` requires the configuration names listed in 
`SmallRyeConfig#getPropertyNames` to find the `Map` keys. If a `ConfigSource` does not support 
`getPropertyNames` (empty), the names must be provided by another `ConfigSource` that can do so. After retrieving the 
map keys, `SmallRyeConfig` performs the lookup of the values with the regular `ConfigSource` ordinal ordering. Even if 
a `ConfigSource` does not provide `getPropertyNames` it can provide the value by having the name listed in another 
capable `ConfigSource`.

For collection types, the key requires the indexed format. The configuration name `server.aliases.localhost[0].name` 
maps to the `Map<String, List<Alias>> aliases()` member, where `localhost` is the `Map` key, `[0]` is the index of the 
`List<Alias>` collection where the `Alias` element will be stored, containing the name `prod`.

!!! info

    They `Map` key part in the configuration property name may require quotes to delimit the key.

### `@WithUnnamedKey`

The `io.smallrye.config.WithUnnamedKey` annotation allows to omit a single map key in the configuration path:

```java
@ConfigMapping(prefix = "server")
public interface Server {
    @WithUnnamedKey("localhost")
    Map<String, Alias> aliases();
    
    interface Alias {
        String name();
    }
}
```

```properties
server.aliases.name=localhost
server.aliases.prod.name=prod
```

The `sever.aliases.name` is an unnamed `Map` property, because it does not contain the `Map` key to populate the `Map` 
entry. Due to `@WithUnnamedKey("localhost")` the `Map` key is not required in the configuration path. The key used to 
look up the Map entry is given by `io.smallrye.config.WithUnnamedKey#value`:

```java
Server server = config.getConfigMapping(Server.class);
Map<String, Alias> localhost = server.aliases.get("localhost");
```

!!! warning

     If the unnamed key (in this case `localhost`) is explicitly set in a property name, the mapping will throw an error.

### `@WithKeys`

The `io.smallrye.config.WithKeys` annotation allows to define which `Map` keys must be loaded by 
the configuration: 

```java
@ConfigMapping(prefix = "server")
public interface Server {
    @WithKeys(KeysProvider.class)
    Map<String, Alias> aliases();
    
    interface Alias {
        String name();
    }

    class KeysProvider implements Supplier<Iterable<String>> {
        @Override
        public Iterable<String> get() {
            return List.of("dev", "test", "prod");
        }
    }
}
```

In this case, `SmallRyeConfig` will look for the map keys `dev`, `test` and `prod` instead of discovering the keys 
with `SmallRyeConfig#getPropertyNames`:

```properties
servers.alias.dev.name=dev
servers.alias.test.name=test
servers.alias.prod.name=prod
```

The provided list will effectively substitute the lookup in `SmallRyeConfig#getPropertyNames`, thus enabling a
`ConfigSource` that does not list its properties, to contribute configuration to the `Map`.  Each key must exist in 
the final configuration (relative to the `Map` path segment), or the mapping will fail with a 
`ConfigValidationException`.

### `@WithDefaults`

The `io.smallrye.config.WithDefaults` is a marker annotation to use only in a `Map` to return the default value for
the value element on any key lookup:

```java
@ConfigMapping(prefix = "server")
public interface Server {
    @WithDefaults
    Map<String, Alias> aliases();
    
    interface Alias {
        @WithDefault("localhost")
        String name();
    }
}
```

```properties
server.aliases.prod.name=prod
```

A look-up to the `aliases` `Map` with the key `localhost`, `any` or any other key, returns a `Alias` instance, where
`Alias.name` is `localhost`, which is the default value. A look-up to `prod` returns a `Alias` instance, where
`Alias.name` is `prod` because the property is defined in the configuration as `server.aliases.prod.name=prod`.

The `Map` can only iterate and size the defined keys. In this case, the `aliases` `Map` only iterates the `prod` key,
and the size is `1`.

```java
Server server = config.getConfigMapping(Server.class);
Map<String, Alias> localhost = server.aliases.get("localhost");
Map<String, Alias> any = server.aliases.get("any");
Map<String, Alias> any = server.aliases.get("prod");
```

## Optionals

- A mapping can wrap any complex type with an `Optional`
- `Optional` mappings do not require the configuration path and value to be present

## Secrets

- A mapping can mark a member type as a secret with `Secret<T>`:

```java
@ConfigMapping(prefix = "credentials")
public interface Credentials {
    String username();

    Secret<String> password();
}
```

A `Secret` value modifies the behaviour of the config system by:

- Omitting the name of the secret in `SmallRyeConfig#getPropertyNames()`
- Omitting the name and value of the secret in the mapping `toString` method
- Throwing a `SecurityException` when trying to retrieve the value via `SmallRyeConfig` programmatic API

A Secret can be of any type that can be converted by a registered `org.eclipse.microprofile.config.spi.Converter` of 
the same type.

## toString, equals, hashcode

If the config mapping contains a `toString` method declaration, the config mapping instance will include a proper
implementation of the `toString` method. The `equals` and `hashcode` methods are included automatically.

!!! caution

    Do not include a `toString` declaration in a config mapping with sensitive information.

## Validation

A config mapping may combine annotations from [Bean Validation](https://beanvalidation.org/) to validate configuration 
properties values.

```java
@ConfigMapping(prefix = "server")
interface Server {
    @Size(min = 2, max = 20)
    String host();

    @Max(10000)
    int port();
}
```

The application startup fails with a `io.smallrye.config.ConfigValidationException` if the configuration properties 
values do not follow the contraints defined in `Server`. 

!!! info
    
    For validation to work, the `smallrye-config-validator` dependency is required in the classpath.
