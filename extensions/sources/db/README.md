[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Database config source

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-db/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-db)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-db.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-db)

An Eclipse MicroProfile config extension which uses a database as source.

## Overview
The eclipse microprofile config framework is a simple yet powerful configuration framework for Java EE. But most implementations only provide the system/env properties or property files as configuration source. This small library provides an ConfigSource implementation which reads the values from the default datasource. For performance reasons, the config values are cached.

> This config source expects JNDI & JDBC to be available. The datasource are looked up

## Usage
```xml
        <dependency>
            <groupId>org.microprofile-ext.config-ext</groupId>
            <artifactId>configsource-db</artifactId>
            <version>1.x</version>
        </dependency>
```

## Configuration
Currently there are 5 values you can configure, either through Java system properties or environment variables:
* **configsource.db.datasource** override ee default datasource by setting JNDI name of the datasource
* **configsource.db.table** table name for configuration records, default value is "configuration"
* **configsource.db.key-column** name of the column containing the key, default value is "key"
* **configsource.db.value-column** name of the column containing the value, default value is "value"
* **configsource.db.validity** how long to cache values (in seconds), default is 30s

### Events

This config source fires CDI Events on changes

Read more about [Config Events](https://github.com/microprofile-extensions/config-ext/blob/master/config-events/README.md)

You can disable this with the `configsource.db.notifyOnChanges` property:

    configsource.db.notifyOnChanges=false

## Links
* https://microprofile.io/project/eclipse/microprofile-config
* https://github.com/rikcarve/consulkv-maven-plugin
* https://github.com/rikcarve/mp-config-consul
* https://github.com/microprofile-extensions

