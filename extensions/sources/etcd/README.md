# Etcd Config Source

Use [etcd](https://coreos.com/etcd/) to get config values. You can define the server details of the etcd server using MicroProfile Config:

## Usage

```xml

    <dependency>
        <groupId>io.smallrye.ext</groupId>
        <artifactId>configsource-etcd</artifactId>
        <version>XXXXXX</version>
        <scope>runtime</scope>
    </dependency>

```

## Configure options

    io.smallrye.ext.config.source.etcd.scheme=http (default)
    io.smallrye.ext.config.source.etcd.host=localhost (default)
    io.smallrye.ext.config.source.etcd.port=2379 (default)
    io.smallrye.ext.config.source.etcd.user (default no user)
    io.smallrye.ext.config.source.etcd.password (default no password)
    io.smallrye.ext.config.source.etcd.authority (default no authority)

You can disable the config source by setting this config:
    
    io.smallrye.ext.config.source.etcd.enabled=false  
