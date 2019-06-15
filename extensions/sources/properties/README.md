[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Properties Config Source

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-properties/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-properties)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-properties.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-properties)

This source gets values from some properties file(s).

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configsource-properties</artifactId>
        <version>XXXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Example:

```properties
    
    somekey=somevalue
    location.protocol=http
    location.host=localhost
    location.port=8080
    location.path=/some/path
    location.jedis=Yoda, Qui-Gon Jinn, Obi-Wan Kenobi, Luke Skywalker
    
```


You can `inject` the jedis using any of the following:

```java

    @Inject
    @ConfigProperty(name = "location.jedis")
    String jedisAsString; 
    
    @Inject
    @ConfigProperty(name = "location.jedis")
    List<String> jedisAsList;
    
    @Inject
    @ConfigProperty(name = "location.jedis")
    Set<String> jedisAsSet;
    
    @Inject
    @ConfigProperty(name = "location.jedis")
    String[] jedisAsArray;

```

## Configure options

### Url(s)

By default the config source will look for a file called `application.properties`. You can set the location(s) of the files:

    configsource.properties.url=<here the url(s)>

example:

    configsource.properties.url=file:/tmp/myconfig.properties

You can also add more than one location by comma-separating the location:

    configsource.properties.url=file:/tmp/myconfig.properties,http://localhost/myconfig.properties

The latest files will override properties in previous files. As example, if using above configuration, property `foo=bar` in `file:/tmp/myconfig.properties` will be override if it's added to `http://localhost/myconfig.properties`.

### Detecting changes.

You can watch the resource for changes. This feature is disabled by default. To enable:

    configsource.properties.pollForChanges=true

By default it will poll every **5 seconds**. You can change that, example to poll every 5 minutes:

    configsource.properties.pollInterval=300

### Events

This config source fires CDI Events on changes (if above detecting for changes is enabled).

Read more about [Config Events](https://github.com/microprofile-extensions/config-ext/blob/master/config-events/README.md)

You can disable this with the `configsource.properties.notifyOnChanges` property:

    configsource.properties.notifyOnChanges=false

If you added more than one resource as source, the event will only fire if the resulting file change also changed the global source change, as one file takes priority over the other.

### Key separator

By default the separator used in the key is a DOT (.) example:

```property
    
    "location.protocol": "http"
```

You can change this by setting `configsource.properties.keyseparator` to the desired separator, example:

    configsource.properties.keyseparator=_

will create:

```property
    
    "location_protocol": "http"
```
