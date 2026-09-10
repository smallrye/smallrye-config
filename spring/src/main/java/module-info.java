module io.smallrye.config.spring {
    requires transitive io.smallrye.config;
    requires io.smallrye.common.annotation;
    requires spring.boot;

    exports io.smallrye.config.spring;

    provides io.smallrye.config.ConfigMappingHandler with
        io.smallrye.config.spring.ConfigurationPropertiesMappingHandler;
}
