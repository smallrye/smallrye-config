# YAML Config Source

This Config Source allows to use a `yaml` file to load configuration values. The YAML Config Source loads the configuration from the following files:

1. (`265`) `application.yaml|yml` in `config` folder, located in the current working directory
2. (`255`) `application.yaml|yml` in the classpath
3. (`110`) MicroProfile Config configuration file `META-INF/microprofile-config.yaml|yml` in the classpath

The following dependency is required in the classpath to use the YAML Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-yaml</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```
