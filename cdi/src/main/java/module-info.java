module io.smallrye.config.inject {
    requires transitive io.smallrye.config;
    requires io.smallrye.config.common;

    requires jakarta.cdi;
    requires jakarta.inject;

    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    exports io.smallrye.config.inject;

    provides jakarta.enterprise.inject.spi.Extension with
        io.smallrye.config.inject.ConfigExtension;
}