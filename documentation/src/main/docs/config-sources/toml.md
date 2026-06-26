# TOML Config Source

This Config Source allows to use a [TOML](https://toml.io) file to load configuration values. The TOML Config Source
loads the configuration from the following files:

1. (`265`) `application.toml` in `config` folder, located in the current working directory
2. (`255`) `application.toml` in the classpath
3. (`110`) MicroProfile Config configuration file `META-INF/microprofile-config.toml` in the classpath

The following dependency is required in the classpath to use the TOML Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-toml</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```
