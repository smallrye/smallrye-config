# Instance Builder

With the `io.smallrye.config.ConfigInstanceBuilder` API, it is possible to create instances of a configuration interface 
programmatically, without requiring a `SmallRyeConfig` or any configuration source. This is useful for testing, default 
configurations, or any scenario where configuration values are known at build time.

## Usage

A configuration interface instance is created with `ConfigInstanceBuilder.forInterface()`:

```java
interface Server {
    String host();

    int port();
}

Server server = ConfigInstanceBuilder.forInterface(Server.class)
        .with(Server::host, "localhost")
        .with(Server::port, 8080)
        .build();
```

The builder uses method references to identify which property to set, providing compile-time type safety without 
string-based property names. The `with` method accepts a method reference to the configuration interface member and the 
value to set.

!!! info

    The configuration interface does not require the `@ConfigMapping` annotation to work with the builder. Any valid 
    configuration interface is accepted.

## Primitive Types

The builder provides dedicated `with()` overloads for `int`, `long`, `double`, and `boolean` primitive types. Other 
primitive types (`byte`, `short`, `float`, `char`) are set via the generic `with()` method using their boxed types:

```java
@ConfigMapping
interface Primitives {
    int intValue();

    boolean booleanValue();

    byte byteValue();
}

Primitives primitives = ConfigInstanceBuilder.forInterface(Primitives.class)
        .with(Primitives::intValue, 42)
        .with(Primitives::booleanValue, true)
        .with(Primitives::byteValue, Byte.valueOf((byte) 1))
        .build();
```

## Optional Properties

The `withOptional()` method sets optional properties. If an optional property is not set, it defaults to empty:

```java
interface AppConfig {
    Optional<String> name();

    OptionalInt timeout();
}

AppConfig config = ConfigInstanceBuilder.forInterface(AppConfig.class)
        .withOptional(AppConfig::name, "MyApp")
        .withOptional(AppConfig::timeout, 30)
        .build();
```

The `withOptional` method wraps the value in `Optional.of()`, `OptionalInt.of()`, `OptionalLong.of()`, or 
`OptionalDouble.of()` depending on the property type.

## Defaults

Properties annotated with `io.smallrye.config.WithDefault` are automatically applied when no value is explicitly set 
in the builder:

```java
interface ServerDefaults {
    @WithDefault("localhost")
    String host();

    @WithDefault("8080")
    int port();
}

ServerDefaults server = ConfigInstanceBuilder.forInterface(ServerDefaults.class).build();
```

The `ServerDefaults#host()` will return the value `localhost` and `ServerDefaults#port()` will return the value `8080`.

Explicitly set values override the `@WithDefault` annotation:

```java
ServerDefaults server = ConfigInstanceBuilder.forInterface(ServerDefaults.class)
        .with(ServerDefaults::host, "0.0.0.0")
        .build();
```

The `ServerDefaults#host()` will return the value `0.0.0.0` and `ServerDefaults#port()` will return the value `8080`.

## Nested Groups

Nested configuration groups are built separately and composed into the parent builder:

```java
interface AppConfig {
    String name();

    DatabaseConfig database();
}

interface DatabaseConfig {
    String url();

    int poolSize();
}

AppConfig config = ConfigInstanceBuilder.forInterface(AppConfig.class)
        .with(AppConfig::name, "MyApp")
        .with(AppConfig::database, ConfigInstanceBuilder.forInterface(DatabaseConfig.class)
                .with(DatabaseConfig::url, "jdbc:h2:mem:test")
                .with(DatabaseConfig::poolSize, 10)
                .build())
        .build();
```

If a nested group has `@WithDefault` values for all its members, the nested group instance is automatically built with 
those defaults when not explicitly set in the builder.

## Collections and Maps

`List`, `Set`, and `Map` types are set directly with their values:

```java
interface AppConfig {
    List<String> hosts();

    Map<String, String> labels();
}

AppConfig config = ConfigInstanceBuilder.forInterface(AppConfig.class)
        .with(AppConfig::hosts, List.of("host1", "host2"))
        .with(AppConfig::labels, Map.of("env", "prod", "region", "us-east"))
        .build();
```

Collections and Maps annotated with `@WithDefault` are automatically populated when not explicitly set.

## Converters

The builder uses the same converter mechanism as `SmallRyeConfig`. Converters are discovered via the 
`java.util.ServiceLoader` mechanism, or can be registered explicitly with `ConfigInstanceBuilder.registerConverter()`:

```java
ConfigInstanceBuilder.registerConverter(Duration.class, new MyDurationConverter());
```

Properties annotated with `io.smallrye.config.WithConverter` use the specified converter for both `@WithDefault` value 
conversion and any value set via the builder:

```java
interface AppConfig {
    @WithDefault("30s")
    @WithConverter(DurationConverter.class)
    Duration timeout();

    @WithConverter(MemorySizeConverter.class)
    MemorySize maxBodySize();
}
```

!!! info

    Converters registered with `registerConverter` are global and shared across all `ConfigInstanceBuilder` instances.

## Required Properties

Properties without a `@WithDefault` are considered required. Calling `build()` throws a `NoSuchElementException` if 
any required property is missing:

```java
interface Server {
    String host();

    @WithDefault("8080")
    int port();
}

// Throws NoSuchElementException because host is required
ConfigInstanceBuilder.forInterface(Server.class).build();

// Works because host is provided
ConfigInstanceBuilder.forInterface(Server.class)
        .with(Server::host, "localhost")
        .build();
```

## Builder Reuse

A builder can be used to produce multiple instances. Each `build()` call creates a new independent instance:

```java
ConfigInstanceBuilder<Server> builder = ConfigInstanceBuilder.forInterface(Server.class)
        .with(Server::host, "localhost")
        .with(Server::port, 8080);

Server first = builder.build();
Server second = builder.build();
```

The `first` and `second` instances are equal but not the same object. Values set after a `build()` call affect only 
subsequent builds.
