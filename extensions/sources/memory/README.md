[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Memory config source

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-memory/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-memory)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-memory.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-memory)

This source gets and sets values in memory. Useful when you want to change config during runtime.

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configsource-memory</artifactId>
        <version>XXXX</version>
    </dependency>

```

## Info

You can do this by using the REST API to change the config values:

```

    GET /microprofile-ext/memoryconfigsource/sources - list all config sources
    GET /microprofile-ext/memoryconfigsource/all - get all configurations
    GET /microprofile-ext/memoryconfigsource/key/{key} - get the configured value for {key}
    PUT /microprofile-ext/memoryconfigsource/key/{key} - set the value for {key}
    DELETE /microprofile-ext/memoryconfigsource/key/{key} - delete the configured value for {key}

```

### Curl Examples

Add a property to `some.key` with value `some value`:

```
    curl -X PUT "http://localhost:8080/config-example/api/microprofile-ext/memoryconfigsource/key/some.key" -H  "accept: */*" -H  "Content-Type: text/plain" -d "some value"
```

Get the property `some.key`:

```
    curl -X GET "http://localhost:8080/config-example/api/microprofile-ext/memoryconfigsource/key/some.key" -H  "accept: */*"
```

Get the property `some.key` but only at the `SystemProperty` source:

```
    curl -X GET "http://localhost:8080/config-example/api/microprofile-ext/memoryconfigsource/key/some.key?configsource=SysPropConfigSource" -H  "accept: */*"
```

Delete the property `some.key`

```
    curl -X DELETE "http://localhost:8080/config-example/api/microprofile-ext/memoryconfigsource/key/some.key" -H  "accept: */*"
```

## Events

This config source fires CDI Events on PUT and DELETE:

Read more about [Config Events](https://github.com/microprofile-extensions/config-ext/blob/master/config-events/README.md)

You can disable this with the `MemoryConfigSource.notifyOnChanges` property

## Configure options

You can disable the config source by setting this config:
    
    MemoryConfigSource.enabled=false

You can disable the change notification eventson changes by setting this config:
    
    MemoryConfigSource.notifyOnChanges=false

![REST API](https://github.com/microprofile-extensions/config-ext/raw/master/configsource-memory/screenshot.png)
