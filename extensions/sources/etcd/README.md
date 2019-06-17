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

    configsource.etcd.scheme=http (default)
    configsource.etcd.host=localhost (default)
    configsource.etcd.port=2379 (default)
    configsource.etcd.user (default no user)
    configsource.etcd.password (default no password)
    configsource.etcd.authority (default no authority)
  

You can disable the config source by setting this config:
    
    EtcdConfigSource.enabled=false  
