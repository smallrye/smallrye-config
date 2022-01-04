# YAML Config Source

This Config Source allows to use a `yaml` file to load configuration values. The YAML Config Source loads the 
configuration from the file `META-INF/microprofile-config.yaml`. It has a higher ordinal (`110`) than the 
`microprofile-config.properties`.

The following dependency is required in the classpath to use the YAML Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-yaml</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```
