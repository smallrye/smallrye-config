# Dynamic Config

Util library to detect and update changes in file based config sources

## Usage

**WARNING:** Use with care. 

You can watch the resource for changes. This feature is disabled by default. 
To enable you need to include the dynamic util jar:

```xml

    <dependency>
        <groupId>io.smallrye.config</groupId>
        <artifactId>smallrye-config-dynamic</artifactId>
        <version>XXXX</version>
        <scope>runtime</scope>
    </dependency>

```

And you also need to set the flag to start using this:

    io.smallrye.config.source.yaml.pollForChanges=true
 
By default it will poll every **5 seconds**.  (Only applicable to HTTP based URLs) You can change that, example to poll every 5 minutes:

    io.smallrye.config.source.yaml.pollInterval=300

When you make a change to the file contents of the config source, the config source will load the new value, and notify via CDI Events.