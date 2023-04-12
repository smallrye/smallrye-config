# Mappings 

With SmallRye Config Mappings, it is possible to group multiple configuration properties in a single interface that 
share the same prefix (or namespace). It supports the following set of features:

- Automatic conversion of the configuration type, including `List`, `Set`, `Map`, `Optional` and primitive types.
- Nested Config Mapping groups.
- Configuration Properties Naming Strategies
- Integration with Bean Validation

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

    If a mapping fails to match a configuration property a `NoSuchElementException` is thrown, unless the mapped 
    element is an `Optional`.

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

    Config Mapping instances are cached. They are populated when the `Config` instance is initialized and their values 
    are not updated on Config Source changes. 

For a Config Mapping to be valid, it needs to match every configuration property name contained in the `Config` under 
the specified prefix set in `@ConfigMapping`. This prevents unknown configuration properties in the `Config`. This 
behaviour can be disabled with the configuration `smallrye.config.mapping.validate-unknown=false`.

## Nested Groups

A nested mapping provides a way to map sub-groups of configuration properties.

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

The `io.smallrye.config.WithParent` annotation allows configurations mappings to inherit its parent container name, 
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

A config mapping is also able to map the collections types `List` and `Set`:

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

## Maps

A config mapping is also able to map a `Map`:

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

For collection types, the key requires the indexed format. The configuration name `server.aliases.localhost[0].name` 
maps to the `Map<String, List<Alias>> aliases()` member, where `localhost` is the `Map` key, `[0]` is the index of the 
`List<Alias>` collection where the `Alias` element will be stored, containing the name `prod`.

!!! info

    They `Map` key part in the configuration property name may require quotes to delimit the key.

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

## ToString

If the config mapping contains a `toString` method declaration, the config mapping instance will include a proper 
implementation of the `toString` method.

!!! caution

    Do not include a `toString` declaration in a config mapping with sensitive information

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

