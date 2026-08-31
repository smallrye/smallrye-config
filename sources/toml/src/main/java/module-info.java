module io.smallrye.config.source.toml {
    requires io.smallrye.common.classloader;
    requires io.smallrye.common.constraint;
    requires transitive io.smallrye.config;
    requires io.smallrye.config.common;

    requires org.tomlj;

    exports io.smallrye.config.source.toml;

    provides io.smallrye.config.ConfigSourceFactory with
        io.smallrye.config.source.toml.TomlLocationConfigSourceFactory;
    provides org.eclipse.microprofile.config.spi.ConfigSourceProvider with
        io.smallrye.config.source.toml.TomlConfigSourceLoader.InClassPath,
        io.smallrye.config.source.toml.TomlConfigSourceLoader.InFileSystem;
}
