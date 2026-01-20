module io.smallrye.config.common {

    requires io.smallrye.common.classloader;

    requires transitive org.eclipse.microprofile.config;

    exports io.smallrye.config.common;
    exports io.smallrye.config.common.utils;
}