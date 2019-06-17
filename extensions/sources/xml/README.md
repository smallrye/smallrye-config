[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Xml Config Source

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-xml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-xml)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-xml.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-xml)

This source gets values from some xml file(s).

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configsource-xml</artifactId>
        <version>XXXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Example:

```xml

    <?xml version="1.0" encoding="UTF-8" ?>
    <root>
        <somekey>somevalue</somekey>
        <location>
            <protocol>http</protocol>
            <host>localhost</host>
            <port>8080</port>
            <path>/some/path</path>
            <jedis>Yoda</jedis>
            <jedis>Qui-Gon Jinn</jedis>
            <jedis>Obi-Wan Kenobi</jedis>
            <jedis>Luke Skywalker</jedis>
        </location>
    </root>
```

will create the following properties:

```property
    
    "somekey": "somevalue"
    "location.protocol": "http"
    "location.host": "localhost"
    "location.port": "8080"
    "location.path": "/some/path"
    "location.jedis": "Yoda, Qui-Gon Jinn, Obi-Wan Kenobi, Luke Skywalker"

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

By default the config source will look for a file called `application.xml`. You can set the location(s) of the files:

    configsource.xml.url=<here the url(s)>

example:

    configsource.xml.url=file:/tmp/myconfig.xml

You can also add more than one location by comma-separating the location:

    configsource.xml.url=file:/tmp/myconfig.xml,http://localhost/myconfig.xml

The latest files will override properties in previous files. As example, if using above configuration, property `foo=bar` in `file:/tmp/myconfig.xml` will be override if it's added to `http://localhost/myconfig.xml`.

### Detecting changes.

You can watch the resource for changes. This feature is disabled by default. To enable:

    configsource.xml.pollForChanges=true

By default it will poll every **5 seconds**. You can change that, example to poll every 5 minutes:

    configsource.xml.pollInterval=300

### Events

This config source fires CDI Events on changes (if above detecting for changes is enabled).

Read more about [Config Events](https://github.com/microprofile-extensions/config-ext/blob/master/config-events/README.md)

You can disable this with the `configsource.xml.notifyOnChanges` property:

    configsource.xml.notifyOnChanges=false

If you added more than one resource as source, the event will only fire if the resulting file change also changed the global source change, as one file takes priority over the other.

### Key separator

By default the separator used in the key is a DOT (.) example:

```property
    
    "location.protocol": "http"
```

You can change this by setting `configsource.xml.keyseparator` to the desired separator, example:

    configsource.xml.keyseparator=_

will create:

```property
    
    "location_protocol": "http"
```
### Include root

By default the root element of the XML is ignored. You can include the root by setting this property:

    configsource.xml.ignoreRoot=false
    
Using the example above, this will create the following properties:

```property
    
    "root.somekey": "somevalue"
    "root.location.protocol": "http"
    "root.location.host": "localhost"
    "root.location.port": "8080"
    "root.location.path": "/some/path"
    "root.location.jedis": "[Yoda, Qui-Gon Jinn, Obi-Wan Kenobi, Luke Skywalker]"    
