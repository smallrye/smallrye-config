module io.smallrye.config.source.hocon {
    requires io.smallrye.common.classloader;
    requires transitive io.smallrye.config;
    requires io.smallrye.config.common;

    requires typesafe.config;

    exports io.smallrye.config.source.hocon;

    provides io.smallrye.config.ConfigSourceFactory with
        io.smallrye.config.source.hocon.HoconConfigSourceFactory;
    provides org.eclipse.microprofile.config.spi.ConfigSourceProvider with
        io.smallrye.config.source.hocon.HoconConfigSourceProvider;
}
