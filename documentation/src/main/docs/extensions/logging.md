## LoggingConfigSourceInterceptor

The `io.smallrye.config.LoggingConfigSourceInterceptor` logs lookups of configuration names in the provided logging 
platform. The log information includes config name and value, the config source origin and location if it exists.

The log is done as `debug`, so the debug threshold must be set to `debug` for the `io.smallrye.config` appender to
display the logs.

This requires registration via the `ServiceLoader` mechanism in the
`META-INF/services/io.smallrye.config.ConfigSourceInterceptorFactory` file of the 
`io.smallrye.config.LoggingConfigSourceInterceptor` interceptor.
