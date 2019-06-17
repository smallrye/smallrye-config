[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Consul Config Source

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-consul/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-consul)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-consul.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-consul)

Use [consul](https://consul.io/) to get config values. You can define the server details of the consul server using MicroProfile Config.
Values are cached to reduce calls to consul. This config source does not support config events (yet).

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configsource-consul</artifactId>
        <version>XXXXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Configure options

    configsource.consul.host (defaults to localhost)
    configsource.consul.prefix (default no prefix)
    configsource.consul.validity (default 30s)
  

You can disable the config source by setting this config:
    
    ConsulConfigSource.enabled=false  

## Links
* https://github.com/rikcarve/consulkv-maven-plugin
* https://microprofile.io/project/eclipse/microprofile-config
