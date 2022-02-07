# Locations

Additionally, to the default config locations specified by the 
[MicroProfile Config](https://github.com/eclipse/microprofile-config/) specification, SmallRye Config provides a way to
scan additional locations for configuration properties files.

The `smallrye.config.locations` configuration property accepts multiple locations separated by a comma `,` and each 
ust represent a valid `URI`. The supported `URI` schemes are:

- file or directory (`file:`)
- classpath resource
- jar resource (`jar:`)
- http resource (`http:`)

Each `URI` scheme loads all discovered resources in a `ConfigSource`. All loaded sources use the same ordinal of the 
source that found the `smallrye.config.locations` configuration property. For instance, if `smallrye.config.locations` 
is set as a system property, then all loaded sources have their ordinals set to `400` (system properties use `400` as
their ordinal). The ordinal may be overridden directly in the resource by setting the `config_ordinal` property. 
Sources are sorted first by their ordinal, then by location order, and finally by loading order.

If a profile is active, and the `URI` represents a single resource (for instance a file), then resources that match 
the active profile are also loaded. The profile resource name must follow the pattern: `{location}-{profile}`. A 
profile resource is only loaded if the unprofiled resource is also available in the same location. This is to keep a 
consistent loading order and pair all the resources together.

Profile resources are not taken into account if the location is a directory since there is no reliable way to discover 
which file is the main resource. Properties that use the profile prefix syntax `%profile.` will work as expected.

_All properties files from a directory_

```properties
# loads all files from a relative path
smallrye.config.locations=./src/main/resources/

# loads all files from an absolute path
smallrye.config.locations=/user/local/config
```

For relative paths, the JVM `user.dir` property defines the current directory.

_A specific file_

```properties
smallrye.config.locations=./src/main/resources/additional.properties
```

If a profile `dev` is active, and an `additional-dev.properties` file exists, this will also be loaded.

_All `additional.properties` files from the classpath_

```properties
smallrye.config.locations=additional.properties
```

If a profile `prod` is active, and an `additional-prod.properties` resources exists next to the `additional.properties` 
resource, this will also be loaded.

_The `resources.properties` file from a specific jar_

```properties
smallrye.config.locations=jar:file:///user/local/app/lib/resources-.jar!/resources.properties
```

If a profile `test` is active, and an `additional-test.properties` respource exists, this will also be loaded.

_The `config.properties` file from a web server_

```properties
smallrye.config.locations=http://localhost:8080/config/config.
```
