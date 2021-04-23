package io.smallrye.config;

import static io.smallrye.config.AbstractLocationConfigSourceFactory.SMALLRYE_LOCATIONS;
import static io.smallrye.config.KeyValuesConfigSource.config;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

class PropertiesLocationConfigSourceFactoryTest {
    @Test
    void systemFile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("dev")
                .withDefaultValue(SMALLRYE_LOCATIONS, "./src/test/resources/additional.properties")
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("more.prop"));
        assertEquals(1, countSources(config));

        config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("dev")
                .withDefaultValue(SMALLRYE_LOCATIONS,
                        Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "additional.properties").toUri()
                                .toString())
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("more.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void systemFolder() {
        SmallRyeConfig config = buildConfig("./src/test/resources");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("5678", config.getRawValue("more.prop"));
        assertEquals(3, countSources(config));
    }

    @Test
    void http() {
        SmallRyeConfig config = buildConfig(
                "https://raw.githubusercontent.com/smallrye/smallrye-config/main/implementation/src/test/resources/config-values.properties");

        assertEquals("abc", config.getRawValue("my.prop"));
        assertEquals(1, countSources(config));
    }

    @Test
    void classpath() {
        SmallRyeConfig config = buildConfig("additional.properties");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals(1, countSources(config));
        assertEquals(500, config.getConfigSources().iterator().next().getOrdinal());
    }

    @Test
    void all() {
        SmallRyeConfig config = buildConfig("./src/test/resources",
                "https://raw.githubusercontent.com/smallrye/smallrye-config/main/implementation/src/test/resources/config-values.properties");

        assertEquals("1234", config.getRawValue("my.prop"));
        assertEquals("5678", config.getRawValue("more.prop"));
        assertEquals(4, countSources(config));
    }

    @Test
    void notFound() {
        SmallRyeConfig config = buildConfig("not.found");

        assertNull(config.getRawValue("my.prop"));
        assertEquals(0, countSources(config));
    }

    @Test
    void noPropertiesFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/random.yml");

        assertEquals(0, countSources(config));
    }

    @Test
    void multipleResourcesInClassPath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my.prop.one=1234\n"), "resources.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset("my.prop.two=5678\n"), "resources.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("resources.properties");

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
                .addAsResource(new StringAsset("my.prop.one=1234\n"), "resources.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = buildConfig("jar:" + filePathOne.toUri() + "!/resources.properties");

            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals(1, countSources(config));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void invalidHttp() {
        assertThrows(IllegalStateException.class,
                () -> buildConfig("https://raw.githubusercontent.com/smallrye/smallrye-config/notfound.properties"));
        buildConfig("https://github.com/smallrye/smallrye-config/blob/3cc4809734d7fbd03852a20b5870ca743a2427bc/pom.xml");
    }

    @Test
    void priorityLoadOrder(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset("my.prop.one=1234\n" +
                        "my.prop.common=1\n" +
                        "my.prop.jar.common=1\n"), "META-INF/microprofile-config.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset("my.prop.two=5678\n" +
                        "my.prop.common=2\n" +
                        "my.prop.jar.common=2\n"), "META-INF/microprofile-config.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        Properties mainProperties = new Properties();
        mainProperties.setProperty("config_ordinal", "100");
        mainProperties.setProperty("my.prop.main", "main");
        mainProperties.setProperty("my.prop.common", "main");
        File mainFile = tempDir.resolve("microprofile-config.properties").toFile();
        try (FileOutputStream out = new FileOutputStream(mainFile)) {
            mainProperties.store(out, null);
        }

        Properties fallbackProperties = new Properties();
        fallbackProperties.setProperty("config_ordinal", "100");
        fallbackProperties.setProperty("my.prop.fallback", "fallback");
        fallbackProperties.setProperty("my.prop.common", "fallback");
        File fallbackFile = tempDir.resolve("fallback.properties").toFile();
        try (FileOutputStream out = new FileOutputStream(fallbackFile)) {
            fallbackProperties.store(out, null);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withDefaultValue(SMALLRYE_LOCATIONS, mainFile.toURI() + "," + fallbackFile.toURI())
                    .build();

            // Check if all sources are up
            assertEquals("1234", config.getRawValue("my.prop.one"));
            assertEquals("5678", config.getRawValue("my.prop.two"));
            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("fallback", config.getRawValue("my.prop.fallback"));
            // This should be loaded by the first defined source in the locations configuration
            assertEquals("main", config.getRawValue("my.prop.common"));
            // This should be loaded by the first discovered source in the classpath
            assertEquals("1", config.getRawValue("my.prop.jar.common"));
            assertEquals(4, countSources(config));
            assertTrue(stream(config.getConfigSources().spliterator(), false)
                    .filter(PropertiesConfigSource.class::isInstance)
                    .allMatch(configSource -> configSource.getOrdinal() == 100));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profiles(@TempDir Path tempDir) throws Exception {
        Properties mainProperties = new Properties();
        mainProperties.setProperty("config_ordinal", "150");
        mainProperties.setProperty("my.prop.main", "main");
        mainProperties.setProperty("my.prop.common", "main");
        mainProperties.setProperty("my.prop.profile", "main");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config.properties").toFile())) {
            mainProperties.store(out, null);
        }

        Properties commonProperties = new Properties();
        commonProperties.setProperty("my.prop.common", "common");
        commonProperties.setProperty("my.prop.profile", "common");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-common.properties").toFile())) {
            commonProperties.store(out, null);
        }

        Properties devProperties = new Properties();
        devProperties.setProperty("my.prop.dev", "dev");
        devProperties.setProperty("my.prop.profile", "dev");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-dev.properties").toFile())) {
            devProperties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("common,dev")
                .withDefaultValue(SMALLRYE_LOCATIONS, tempDir.resolve("config.properties").toUri().toString())
                .build();

        assertEquals("main", config.getRawValue("my.prop.main"));
        assertEquals("common", config.getRawValue("my.prop.common"));
        assertEquals("dev", config.getRawValue("my.prop.profile"));
    }

    @Test
    void onlyProfileFile(@TempDir Path tempDir) throws Exception {
        Properties devProperties = new Properties();
        devProperties.setProperty("my.prop.dev", "dev");
        devProperties.setProperty("my.prop.profile", "dev");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-dev.properties").toFile())) {
            devProperties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("common,dev")
                .withDefaultValue(SMALLRYE_LOCATIONS, tempDir.resolve("config.properties").toUri().toString())
                .build();

        assertNull(config.getRawValue("my.prop.profile"));
    }

    @Test
    void profilesClasspath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common\n" +
                                "my.prop.profile=common\n"),
                        "META-INF/config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev\n" +
                                "my.prop.profile=dev\n"),
                        "META-INF/config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/config.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_LOCATIONS, "META-INF/config.properties")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("common", config.getRawValue("my.prop.common"));
            assertEquals("dev", config.getRawValue("my.prop.profile"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profilesJar(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common\n" +
                                "my.prop.profile=common\n"),
                        "META-INF/config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev\n" +
                                "my.prop.profile=dev\n"),
                        "META-INF/config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_LOCATIONS, "jar:" + filePathOne.toUri() + "!/META-INF/config.properties")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("common", config.getRawValue("my.prop.common"));
            assertEquals("dev", config.getRawValue("my.prop.profile"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profilesHttp() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/config.properties", exchange -> {
            Properties mainProperties = new Properties();
            mainProperties.setProperty("config_ordinal", "150");
            mainProperties.setProperty("my.prop.main", "main");
            mainProperties.setProperty("my.prop.common", "main");
            mainProperties.setProperty("my.prop.profile", "main");
            StringWriter writer = new StringWriter();
            mainProperties.store(writer, null);
            byte[] bytes = writer.toString().getBytes();
            exchange.sendResponseHeaders(200, writer.toString().length());
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
        });

        server.createContext("/config-common.properties", exchange -> {
            Properties commonProperties = new Properties();
            commonProperties.setProperty("my.prop.common", "common");
            commonProperties.setProperty("my.prop.profile", "common");
            StringWriter writer = new StringWriter();
            commonProperties.store(writer, null);
            byte[] bytes = writer.toString().getBytes();
            exchange.sendResponseHeaders(200, writer.toString().length());
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
        });

        server.createContext("/config-dev.properties", exchange -> {
            Properties devProperties = new Properties();
            devProperties.setProperty("my.prop.dev", "dev");
            devProperties.setProperty("my.prop.profile", "dev");
            StringWriter writer = new StringWriter();
            devProperties.store(writer, null);
            byte[] bytes = writer.toString().getBytes();
            exchange.sendResponseHeaders(200, writer.toString().length());
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
        });

        server.start();

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("common,dev,unknown")
                .withDefaultValue(SMALLRYE_LOCATIONS,
                        "http://localhost:" + server.getAddress().getPort() + "/config.properties")
                .build();

        assertEquals("main", config.getRawValue("my.prop.main"));
        assertEquals("common", config.getRawValue("my.prop.common"));
        assertEquals("dev", config.getRawValue("my.prop.profile"));

        server.stop(0);
    }

    @Test
    void mixedProfiles(@TempDir Path tempDir) throws Exception {
        Properties mainProperties = new Properties();
        mainProperties.setProperty("config_ordinal", "150");
        mainProperties.setProperty("my.prop.main", "main-file");
        mainProperties.setProperty("my.prop.main.file", "main-file");
        mainProperties.setProperty("my.prop.common", "main-file");
        mainProperties.setProperty("my.prop.profile", "main-file");
        mainProperties.setProperty("order", "5");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config.properties").toFile())) {
            mainProperties.store(out, null);
        }

        Properties commonProperties = new Properties();
        commonProperties.setProperty("my.prop.common", "common-file");
        commonProperties.setProperty("my.prop.profile", "common-file");
        commonProperties.setProperty("order", "3");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-common.properties").toFile())) {
            commonProperties.store(out, null);
        }

        Properties devProperties = new Properties();
        devProperties.setProperty("my.prop.dev", "dev-file");
        devProperties.setProperty("my.prop.profile", "dev-file");
        devProperties.setProperty("order", "1");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-dev.properties").toFile())) {
            devProperties.store(out, null);
        }

        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main-cp\n" +
                                "my.prop.main.cp=main-cp\n" +
                                "my.prop.common=main-cp\n" +
                                "my.prop.profile=main-cp\n" +
                                "order=6\n"),
                        "config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common-cp\n" +
                                "my.prop.profile=common-cp\n" +
                                "order=4\n"),
                        "config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev-cp\n" +
                                "my.prop.profile=dev-cp\n" +
                                "order=2\n"),
                        "config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_LOCATIONS,
                            tempDir.resolve("config.properties").toUri() + "," + "config.properties")
                    .build();

            assertEquals("main-file", config.getRawValue("my.prop.main.file"));
            assertEquals("main-cp", config.getRawValue("my.prop.main.cp"));
            assertEquals("main-file", config.getRawValue("my.prop.main"));
            assertEquals("common-file", config.getRawValue("my.prop.common"));
            assertEquals("dev-file", config.getRawValue("my.prop.profile"));

            final List<ConfigSource> sources = stream(config.getConfigSources().spliterator(), false)
                    .filter(PropertiesConfigSource.class::isInstance).collect(toList());
            assertEquals(6, sources.size());
            assertEquals("1", sources.get(0).getValue("order"));
            assertEquals("2", sources.get(1).getValue("order"));
            assertEquals("3", sources.get(2).getValue("order"));
            assertEquals("4", sources.get(3).getValue("order"));
            assertEquals("5", sources.get(4).getValue("order"));
            assertEquals("6", sources.get(5).getValue("order"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void illegalChars(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources- space -one.jar")
                .addAsResource(new StringAsset("my.prop.main=main"), "META-INF/config.properties")
                .addAsResource(new StringAsset("my.prop.common=common"), "META-INF/config-common.properties")
                .addAsResource(new StringAsset("my.prop.dev=dev"), "META-INF/config-dev.properties");

        Path filePathOne = tempDir.resolve("resources- space -one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .withDefaultValue(SMALLRYE_LOCATIONS, "META-INF/config.properties")
                    .build();

            assertEquals("main", config.getRawValue("my.prop.main"));
            assertEquals("common", config.getRawValue("my.prop.common"));
            assertEquals("dev", config.getRawValue("my.prop.dev"));
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void ordinal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withSources(config(SMALLRYE_LOCATIONS, "more.properties", "config_ordinal", "1000"))
                .build();

        assertEquals("5678", config.getConfigValue("more.prop").getValue());
        assertEquals(1000, config.getConfigValue("more.prop").getConfigSourceOrdinal());
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_LOCATIONS, String.join(",", locations))
                .build();
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

    private static int countSources(SmallRyeConfig config) {
        return (int) stream(config.getConfigSources().spliterator(), false).filter(PropertiesConfigSource.class::isInstance)
                .count();
    }
}
