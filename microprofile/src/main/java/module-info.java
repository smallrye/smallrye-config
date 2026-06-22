module io.smallrye.config.microprofile {
    requires transitive io.smallrye.config;

    exports io.smallrye.config.microprofile;

    provides io.smallrye.config.ConfigMappingHandler with
        io.smallrye.config.microprofile.ConfigPropertiesConfigMappingHandler;
}
