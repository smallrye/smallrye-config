open module io.smallrye.config.source.yaml.test {
    requires transitive io.smallrye.config.source.yaml;

    requires io.smallrye.config;

    requires java.logging;

    requires org.eclipse.microprofile.config;
    requires org.junit.jupiter.api;
    requires org.yaml.snakeyaml;

    requires io.smallrye.testing.utilities;
}