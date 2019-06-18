# Memory config source

This source gets and sets values in memory. Useful when you want to change config during runtime.

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.ext</groupId>
        <artifactId>configsource-memory</artifactId>
        <version>XXXX</version>
    </dependency>

```

## Info

You can do this by using the REST API to change the config values:

```

    GET /smallrye/config/sources - list all config sources
    GET /smallrye/config/all - get all configurations
    GET /smallrye/config/key/{key} - get the configured value for {key}
    PUT /smallrye/config/key/{key} - set the value for {key}
    DELETE /smallrye/config/key/{key} - delete the configured value for {key}

```

### Curl Examples

Add a property to `some.key` with value `some value`:

```
    curl -X PUT "http://localhost:8080/config-example/api/smallrye/config/key/some.key" -H  "accept: */*" -H  "Content-Type: text/plain" -d "some value"
```

Get the property `some.key`:

```
    curl -X GET "http://localhost:8080/config-example/api/smallrye/config/key/some.key" -H  "accept: */*"
```

Get the property `some.key` but only at the `SystemProperty` source:

```
    curl -X GET "http://localhost:8080/config-example/api/smallrye/config/key/some.key?configsource=SysPropConfigSource" -H  "accept: */*"
```

Delete the property `some.key`

```
    curl -X DELETE "http://localhost:8080/config-example/api/smallrye/config/key/some.key" -H  "accept: */*"
```

## Events

This config source fires CDI Events on PUT and DELETE:

Read more about [Config Events](https://github.com/smallrye/smallrye-config/tree/master/extensions/utils/events)

You can disable this with the `io.smallrye.ext.config.source.memory.notifyOnChanges` property

## Configure options

You can disable the config source by setting this config:
    
    io.smallrye.ext.config.source.memory.enabled=false

You can disable the change notification events on changes by setting this config:
    
    io.smallrye.ext.config.source.memory.notifyOnChanges=false

![REST API](https://raw.githubusercontent.com/smallrye/smallrye-config/master/extensions/sources/memory/screenshot.png)
