# Getting Started

## Config Sources

By default, SmallRye Config reads configuration properties from multiple configuration sources (by descending ordinal):

1. (400) System properties
2. (300) Environment variables
3. (100) MicroProfile Config configuration file `META-INF/microprofile-config.properties` in the classpath

A configuration source is handled by a `ConfigSource`. A `ConfigSource` provides configuration values from a specific
place.  

The final configuration is the aggregation of the properties defined by all these sources. A configuration property 
lookup starts by the highest ordinal configuration source available and works it way down to other sources until a 
match is found. This means that any configuration property may override a value just by setting a different value in a 
higher ordinal config source. For example, a property configured using an environment property overrides the value 
provided using the `microprofile-config.properties` file.

### System Properties

System properties can be handed to the application through the `-D` flag during startup. For instance, 
`java -Dmy.prop -jar my.jar`.

### Env Properties

Environment variables are set directly in the host operating system. Environment variables names follow the conversion 
rules specified by [MicroProfile Config](https://github.com/eclipse/microprofile-config/).
 
### MicroProfile Config configuration file

The MicroProfile Config configuration file `META-INF/microprofile-config.properties` in the classpath. It follows the 
standard convention for `properties` files.

```properties
greeting.message=hello
goodbye.message=bye
```

### Additional Config Sources

SmallRye Config provides additional extensions which cover other configuration formats and stores:

- [YAML](../config-sources/yaml.md)
- [File System](../config-sources/filesystem.md)
- [ZooKeeper](../config-sources/zookeeper.md)
- [HOCON](../config-sources/hocon.md)

It is also possible to create a Custom Config Source.

## Retrieving the Configuration

### Programmatically

The `org.eclipse.microprofile.config.ConfigProvider.getConfig()` API allows to access the 
`org.eclipse.microprofile.config.Config` API programmatically.

```java
Config config = ConfigProvider.getConfig();

String message = config.getValue("greeting.message", String.class);
```

The `Config` instance will be created and registered to the current context class loader if no such configuration is 
already created and registered. This means that subsequent calls to `ConfigProvider.getConfig()` will return the same 
`Config` instance if the context class loader is the same.

To obtain a detached instanced, use the `io.smallrye.config.SmallRyeConfigBuilder`:

```java
SmallRyeConfig config = new SmallRyeConfigBuilder()
    .addDefaultInterceptors()
    .addDefaultSources()
    .build();

String message = config.getValue("greeting.message", String.class);
```

### With CDI

In a CDI environment, configuration can be injected in CDI aware beans with `@Inject` and 
the `org.eclipse.microprofile.config.inject.ConfigProperty` qualifier.

```java
@Inject
@ConfigProperty(name = "greeting.message") 
String message;

@Inject
@ConfigProperty(name = "greeting.suffix", defaultValue="!") 
String suffix;

@Inject
@ConfigProperty(name = "greeting.name")
Optional<String> name; 
```

- If a value if not provided for this `greeting.message`, the application startup fails with a 
`javax.enterprise.inject.spi.DeploymentException: No config value of type [class java.lang.String] exists for: greeting.message`.
- The default value `!` is injected if the configuration does not provide a value for `greeting.suffix`.
- The property `greeting.name` is optional - an empty Optional is injected if the configuration does not provide a 
value for it.   

## Config vs SmallRyeConfig

The `io.smallrye.config.SmallRyeConfig` is an implementation of `org.eclipse.microprofile.config.Config` and provides 
additional APIs and helper methods not available in `org.eclipse.microprofile.config.Config`. To obtain an instance of 
`io.smallrye.config.SmallRyeConfig`, the original `org.eclipse.microprofile.config.Config` can be unwrapped:

```java
Config config = ConfigProvider.getConfig();
SmallRyeConfig smallRyeConfig = config.unwrap(SmallRyeConfig.class);
```

Or if using the builder it can be obtained directly:

```java
SmallRyeConfig config = new SmallRyeConfigBuilder().build();
```

A few notable APIs provided by `io.smallrye.config.SmallRyeConfig` allow to:

- Retrive multiple values into a specified `Collection`
- Retrive [Indexed Values](indexed-properties.md)
- Retrive [Config Mappings](mappings.md) instances
- Retrieve the raw value of a configuration
- Check if a property is present
- Retrieve a `Converter`
- Convert values

## Converters

The `ConfigSource` retrieves a configuration value as a `String`. Other data types require a conversion using the 
`org.eclipse.microprofile.config.spi.Converter` API.

Most of the common `Converter` types are provided by default:

* `boolean` and `java.lang.Boolean`; the values "true", "1", "YES", "Y" "ON" represent `true`. Any other value will be 
interpreted as `false`
* `byte` and `java.lang.Byte`
* `short` and `java.lang.Short`
* `int`, `java.lang.Integer`, and `java.util.OptionalInt`
* `long`, `java.lang.Long`, and `java.util.OptionalLong`
* `float` and `java.lang.Float`; a dot '.' is used to separate the fractional digits
* `double`, `java.lang.Double`, and `java.util.OptionalDouble`; a dot '.' is used to separate the fractional digits
* `char` and `java.lang.Character`
* `java.lang.Class` based on the result of `Class.forName`
* `java.net.InetAddress`
* `java.util.UUID`
* `java.util.Currency`
* `java.util.regex.Pattern`
* Any class with declared static methods `of`, `valueOf` or `parse` that take a `String` or a `CharSequence`
* Any class with declared constructors that takes a `String` or a `CharSequence` 

All default converters have a priority of `1`.
