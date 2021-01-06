package io.smallrye.config.source.yaml;

import static io.smallrye.config.AbstractLocationConfigSourceFactory.SMALLRYE_LOCATIONS;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class YamlLocationConfigSourceFactoryTest {
    @Test
    void systemFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/additional.yml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("more.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void systemFolder() {
        SmallRyeConfig config = buildConfig("./src/test/resources");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("5678", config.getRawValue("more.prop"));
        assertEquals(7, countSources(config));
    }

    @Test
    void webResource() {
        SmallRyeConfig config = buildConfig(
                "https://raw.githubusercontent.com/smallrye/smallrye-config/master/sources/yaml/src/test/resources/example-profiles.yml");

        assertEquals("default", config.getRawValue("foo.bar"));
        assertEquals(1, countSources(config));
    }

    @Test
    void classpath() {
        SmallRyeConfig config = buildConfig("additional.yml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void all() {
        SmallRyeConfig config = buildConfig("./src/test/resources",
                "https://raw.githubusercontent.com/smallrye/smallrye-config/master/sources/yaml/src/test/resources/example-profiles.yml");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("5678", config.getRawValue("more.prop"));
        assertEquals(8, countSources(config));
    }

    @Test
    void notFound() {
        SmallRyeConfig config = buildConfig("not.found");

        assertNull(config.getRawValue("my.prop"));
        assertEquals(0, countSources(config));
    }

    @Test
    void noPropertiesFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/random.properties");

        assertEquals(0, countSources(config));
    }

    @Test
    void multipleResourcesInClassPath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    one: 1234\n"), "resources.yml");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    two: 5678\n"), "resources.yml");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePathOne.toUri() + "!/"),
                new URL("jar:" + filePathTwo.toUri() + "!/"),
        }, contextClassLoader)) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("resources.yml");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals("5678", config.getRawValue("my.prop.two"));
            assertEquals(2, countSources(config));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void jar(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my:\n" +
                        "  prop:\n" +
                        "    one: 1234\n"), "resources.yml");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePathOne.toUri() + "!/")
        }, contextClassLoader)) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("jar:" + filePathOne.toUri() + "!/resources.yml");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals(1, countSources(config));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void invalidWebResource() {
        assertThrows(IllegalStateException.class,
                () -> buildConfig("https://raw.githubusercontent.com/smallrye/smallrye-config/notfound.yml"));
        buildConfig("https://github.com/smallrye/smallrye-config/blob/3cc4809734d7fbd03852a20b5870ca743a2427bc/pom.xml");
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_LOCATIONS, String.join(",", locations))
                .build();
    }

    private static int countSources(SmallRyeConfig config) {
        return (int) stream(config.getConfigSources().spliterator(), false).filter(
                YamlConfigSource.class::isInstance).count();
    }
}
