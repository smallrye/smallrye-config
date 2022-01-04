# Config Source Factory

Another way to create a `ConfigSource` is via the `ConfigSourceFactory`. The difference between the SmallRye Factory
and the standard way to create a `ConfigSource` as specified in MicroProfile Config, is the Factory ability to provide
a context with access to the available configuration. With the `ConfigSourceFactory` it is possible to bootstrap a
`ConfigSource` that configures itself with previously initialized `ConfigSources`.

By implementing https://github.com/smallrye/smallrye-config/blob/main/implementation/src/main/java/io/smallrye/config/ConfigSourceFactory.java[ConfigSourceFactory],
a list of `ConfigSources` may be provided via the `Iterable<ConfigSource> getConfigSources(ConfigSourceContext context)`
method. The `ConfigSourceFactory` may also assign a priority by overriding the default method
`OptionalInt getPriority()`. This is only used to sort multiple factories during initialization. Once all the factories
are initialized, the provided `ConfigSources` will use their own ordinal and be sorted with all `ConfigSources`
available in the `Config` instance.

When the Factory is initializing, the provided `ConfigSourceContext` may call the method
`ConfigValue getValue(String name)`. This method lookups configuration names in all `ConfigSources` that were already
initialized by the `Config` instance, including sources with lower ordinals than the ones defined in the
`ConfigSourceFactory`. `ConfigSources` provided by a `ConfigSourceFactory` is not taken into account to configure other
sources produced by a lower priority `ConfigSourceFactory`.

Registration of a `ConfigSourceFactory` is done via the `ServiceLoader` mechanism by providing the
implementation classes in a `META-INF/services/io.smallrye.config.ConfigSourceFactory` file. Alternatively, factories
may be registered via the Programmatic API in `SmallRyeConfigBuilder#withSources`.
