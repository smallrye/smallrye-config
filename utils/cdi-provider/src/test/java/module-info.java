open module io.smallrye.config.source.injection.test {
    requires io.smallrye.config;
    requires io.smallrye.config.inject;
    requires io.smallrye.config.util.injection;

    requires jakarta.inject;
    requires jakarta.cdi;

    requires org.eclipse.microprofile.config;
    requires org.junit.jupiter.api;

    // even though we can't run in module mode, we still need to build this way
    requires weld.junit5;
}