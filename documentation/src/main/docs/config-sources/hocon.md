# HOCON Config Source

This Config Source allows to use the [HOCON](https://github.com/lightbend/config/blob/main/HOCON.md) file format to 
load configuration values. The HOCON Config Source loads the
configuration from the file `META-INF/microprofile-config.conf`. It has a lower ordinal (`50`) than the
`microprofile-config.properties`.

The following dependency is required in the classpath to use the HOCON Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-hocon</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```
