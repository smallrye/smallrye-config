package io.smallrye.config.test.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class SmallRyeConfigBuilderCustomizerTest {
    @Test
    void builder() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withCustomizers(new CustomConfigBuilder())
                .build();

        assertEquals("1234", config.getRawValue("from.custom.builder"));
    }

    @Test
    void discoveredBuilder(@TempDir Path tempDir) throws Exception {
        JavaArchive serviceJar = ShrinkWrap
                .create(JavaArchive.class, "service.jar")
                .addAsManifestResource(new StringAsset("io.smallrye.config.test.builder.CustomConfigBuilder"),
                        "services/io.smallrye.config.SmallRyeConfigBuilderCustomizer");

        Path servidePath = tempDir.resolve("resources-one.jar");
        serviceJar.as(ZipExporter.class).exportTo(servidePath.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + servidePath.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder().addDiscoveredCustomizers().build();

            assertEquals("1234", config.getRawValue("from.custom.builder"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void priority(@TempDir Path tempDir) throws Exception {
        JavaArchive serviceJar = ShrinkWrap
                .create(JavaArchive.class, "service.jar")
                .addAsManifestResource(new StringAsset(
                        "io.smallrye.config.test.builder.CustomOneConfigBuilder\n" +
                                "io.smallrye.config.test.builder.CustomTwoConfigBuilder\n"),
                        "services/io.smallrye.config.SmallRyeConfigBuilderCustomizer");

        Path servidePath = tempDir.resolve("resources-one.jar");
        serviceJar.as(ZipExporter.class).exportTo(servidePath.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + servidePath.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder().addDiscoveredCustomizers().build();

            assertEquals("two", config.getRawValue("one"));
            assertEquals("true", config.getRawValue("addDefaultSources"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private static URLClassLoader urlClassLoader(ClassLoader parent, String... urls) {
        return new URLClassLoader(Stream.of(urls).map(spec -> {
            try {
                return new URL(spec);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }).toArray(URL[]::new), parent);
    }
}
