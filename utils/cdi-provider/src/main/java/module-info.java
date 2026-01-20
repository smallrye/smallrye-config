module io.smallrye.config.util.injection {
    requires jakarta.annotation;
    requires jakarta.cdi;
    requires jakarta.inject;

    requires transitive org.eclipse.microprofile.config;

    exports io.smallrye.config.util.injection;
}