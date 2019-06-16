[Back to config-ext](https://github.com/microprofile-extensions/config-ext/blob/master/README.md)

# Yaml Config Source

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-yaml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.microprofile-ext.config-ext/configsource-yaml)
[![Javadocs](https://www.javadoc.io/badge/org.microprofile-ext.config-ext/configsource-yaml.svg)](https://www.javadoc.io/doc/org.microprofile-ext.config-ext/configsource-yaml)

This source gets values from some yaml file(s).

## Usage

```xml

    <dependency>
        <groupId>org.microprofile-ext.config-ext</groupId>
        <artifactId>configsource-yaml</artifactId>
        <version>XXXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Example:

```yaml

    location:
        protocol: "http"
        host: "localhost"
        port: "8080"
        path: "/some/path"
        jedis:
            - Yoda
            - Qui-Gon Jinn
            - Obi-Wan Kenobi
            - Luke Skywalker
```

will create the following properties:

```property
    
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

By default the config source will look for a file called `application.yaml`. You can set the location(s) of the files:

    configsource.yaml.url=<here the url(s)>

example:

    configsource.yaml.url=file:/tmp/myconfig.yml

You can also add more than one location by comma-separating the location:

    configsource.yaml.url=file:/tmp/myconfig.yml,http://localhost/myconfig.yml

The latest files will override properties in previous files. As example, if using above configuration, property `foo=bar` in `file:/tmp/myconfig.yml` will be override if it's added to `http://localhost/myconfig.yml`.

### Detecting changes.

You can watch the resource for changes. This feature is disabled by default. To enable:

    configsource.yaml.pollForChanges=true

By default it will poll every **5 seconds**. You can change that, example to poll every 5 minutes:

    configsource.yaml.pollInterval=300

### Events

This config source fires CDI Events on changes (if above detecting for changes is enabled).

Read more about [Config Events](https://github.com/microprofile-extensions/config-ext/blob/master/config-events/README.md)

You can disable this with the `configsource.yaml.notifyOnChanges` property:

    configsource.yaml.notifyOnChanges=false

If you added more than one resource as source, the event will only fire if the resulting file change also changed the global source change, as one file takes priority over the other.

### Key separator

By default the separator used in the key is a DOT (.) example:

```property
    
    "location.protocol": "http"
```

You can change this by setting `configsource.yaml.keyseparator` to the desired separator, example:

    configsource.yaml.keyseparator=_

will create:

```property
    
    "location_protocol": "http"
```
