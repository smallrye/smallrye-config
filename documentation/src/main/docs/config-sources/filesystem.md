# FileSystem Config Source

This Config Source loads configuration values for each file found in a directory. Each file corresponds to a single 
property, where the file name is the configuration property name and the file content the configuration value.

For instance, if a directory structure looks like:

    foo/
    |__num.max
    |__num.size

The `FileSystem` Config Source will provide 2 properties:

-   num.max
-   num.size

!!! warning

    Nested directories are not supported.

This Config Source can be used to read configuration from
[Kubernetes ConfigMap](https://kubernetes.io/docs/tasks/configure-pod-container/configure-pod-configmap). Check the 
[Kubernetes ConfigMap ConfigSource Example](https://github.com/smallrye/smallrye-config/blob/main/examples/configmap/README.adoc).

The same mapping rules as defined for environment variables are applied, so the `FileSystem` Config Source will search 
for a given property name `num.max`:

- Exact match (`num.max`)
- Replace each character that is neither alphanumeric nor \_ with \_ (`num_max`)
- Replace each character that is neither alphanumeric nor \_ with \_; then convert the name to upper case (`NUM_MAX`)

The following dependency is required in the classpath to use the `FileSystem` Config Source:

```xml
<dependency>
    <groupId>io.smallrye.config</groupId>
    <artifactId>smallrye-config-source-file-system</artifactId>
    <version>{{attributes['version']}}</version>
</dependency>
```

The configuration property `smallrye.config.source.file.locations` sets the directory paths to look up the files. It 
accepts multiple locations separated by a comma and each must represent a valid URI to a directory.
