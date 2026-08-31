open module io.smallrye.config.source.toml.test {
    requires transitive io.smallrye.config.source.toml;

    requires io.smallrye.config;

    requires java.logging;

    requires org.eclipse.microprofile.config;
    requires org.junit.jupiter.api;
    requires org.tomlj;

    requires io.smallrye.testing.utilities;
}
