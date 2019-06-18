# Consul Config Source

Use [consul](https://consul.io/) to get config values. You can define the server details of the consul server using MicroProfile Config.
Values are cached to reduce calls to consul. This config source does not support config events (yet).

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.ext</groupId>
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
    
    io.smallrye.ext.config.source.consul.enabled=false  

## Links
* https://github.com/rikcarve/consulkv-maven-plugin
* https://microprofile.io/project/eclipse/microprofile-config