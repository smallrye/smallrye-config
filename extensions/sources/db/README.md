# Database config source

An Eclipse MicroProfile config extension which uses a database as source.

## Overview
The eclipse microprofile config framework is a simple yet powerful configuration framework for Java EE. But most implementations only provide the system/env properties or property files as configuration source. This small library provides an ConfigSource implementation which reads the values from the default datasource. For performance reasons, the config values are cached.

> This config source expects JNDI & JDBC to be available. The datasource are looked up

## Usage
```xml
        <dependency>
            <groupId>io.smallrye.ext</groupId>
            <artifactId>configsource-db</artifactId>
            <version>1.x</version>
        </dependency>
```

## Configuration
Currently there are 5 values you can configure, either through Java system properties or environment variables:
* **io.smallrye.config.source.db.datasource** override ee default datasource by setting JNDI name of the datasource
* **io.smallrye.config.source.db.table** table name for configuration records, default value is "configuration"
* **io.smallrye.config.source.db.key-column** name of the column containing the key, default value is "key"
* **io.smallrye.config.source.db.value-column** name of the column containing the value, default value is "value"
* **io.smallrye.config.source.db.validity** how long to cache values (in seconds), default is 30s

You can disable the config source by setting this config:
    
    io.smallrye.config.source.db.enabled=false

### Events

This config source fires CDI Events on changes

Read more about [Config Events](https://github.com/smallrye/smallrye-config/tree/master/extensions/utils/events)

You can disable this with the `io.smallrye.config.source.db.notifyOnChanges` property:

    io.smallrye.config.source.db.notifyOnChanges=false

## Links
* https://microprofile.io/project/eclipse/microprofile-config
* https://github.com/rikcarve/consulkv-maven-plugin
* https://github.com/rikcarve/mp-config-consul
* https://github.com/microprofile-extensions

