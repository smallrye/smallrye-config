module io.smallrye.config.source.yaml {
    requires io.smallrye.common.classloader;
    requires io.smallrye.common.constraint;
    requires transitive io.smallrye.config;
    requires io.smallrye.config.common;

    requires org.yaml.snakeyaml;

    exports io.smallrye.config.source.yaml;

    provides io.smallrye.config.ConfigSourceFactory with
        io.smallrye.config.source.yaml.YamlLocationConfigSourceFactory;
    provides org.eclipse.microprofile.config.spi.ConfigSourceProvider with
        io.smallrye.config.source.yaml.YamlConfigSourceLoader.InClassPath,
        io.smallrye.config.source.yaml.YamlConfigSourceLoader.InFileSystem;
}