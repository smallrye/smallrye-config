# ZooKeeper Config Source

This Config Source allows using [Apache ZooKeeper](https://zookeeper.apache.org/index.html) to load configuration 
values.

The following dependency is required in the classpath to use the ZooKeeper Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-zookeeper</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

It also requires to set up additional configuration properties to identify the ZooKeeper instance:

```properties
io.smallrye.configsource.zookeeper.url=localhost:2181
io.smallrye.configsource.zookeeper.applicationId=applicationId
```

The ZooKeeper Config Source will look for configuration values in a ZooKeeper instance running in the url set in 
`io.smallrye.configsource.zookeeper.url` and in the znodes available in `/applicationId/`.

This Config Source has an ordinal of `150`.
