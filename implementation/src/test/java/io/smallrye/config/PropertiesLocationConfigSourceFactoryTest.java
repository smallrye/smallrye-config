package io.smallrye.config;

import static io.smallrye.config.KeyValuesConfigSource.config;
import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import io.smallrye.config.common.MapBackedConfigSource;
import io.smallrye.testing.logging.LogCapture;

class PropertiesLocationConfigSourceFactoryTest {
    @RegisterExtension
    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord.getMessage().startsWith("SRCFG"), Level.ALL);

    @BeforeEach
    void setUp() {
        logCapture.records().clear();
    }

    @Test
    void systemFile() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("dev")
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, "./src/test/resources/additional.properties")
                .build();

        assertEquals("1234", config.getRawValue("my.prop"));
        assertNull(config.getRawValue("more.prop"));
        assertEquals(1, countSources(config));

        config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("dev")
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS,
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
        assertEquals(4, countSources(config));
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
        assertEquals(5, countSources(config));
    }

    @Test
    void notFound() {
        SmallRyeConfig config = buildConfig("not.found");

        assertNull(config.getRawValue("my.prop"));
        assertEquals(0, countSources(config));

        assertThrows(IllegalStateException.class, () -> buildConfig("file:/not/found/not-found.properties"),
                "Failed to load resource file:/not/found/not-found.properties");
        assertThrows(IllegalStateException.class, () -> buildConfig("http://not.found/not-found-properties"),
                "Failed to load resource http://not.found/not-found-properties");
        assertThrows(IllegalStateException.class, () -> buildConfig("jar:file:/resources-one.jar!/"),
                "Failed to load resource jar:file:/resources-one.jar!/");
    }

    @Test
    void noPropertiesFile() {
        SmallRyeConfig config = buildConfig("./src/test/resources/random.yml");

        assertEquals(0, countSources(config));
    }

    @Test
    void invalidHttp() {
        assertThrows(IllegalStateException.class,
                () -> buildConfig("https://raw.githubusercontent.com/smallrye/smallrye-config/notfound.properties"));
        buildConfig("https://github.com/smallrye/smallrye-config/blob/3cc4809734d7fbd03852a20b5870ca743a2427bc/pom.xml");
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
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, tempDir.resolve("config.properties").toUri().toString())
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

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("common,dev")
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, tempDir.resolve("config.properties").toUri().toString());

        assertThrows(IllegalStateException.class, builder::build);
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
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS,
                        "http://localhost:" + server.getAddress().getPort() + "/config.properties")
                .build();

        assertEquals("main", config.getRawValue("my.prop.main"));
        assertEquals("common", config.getRawValue("my.prop.common"));
        assertEquals("dev", config.getRawValue("my.prop.profile"));

        server.stop(0);
    }

    @Test
    void ordinal() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withSources(config(SMALLRYE_CONFIG_LOCATIONS, "more.properties", "config_ordinal", "1000"))
                .build();

        assertEquals("5678", config.getConfigValue("more.prop").getValue());
        assertEquals(1000, config.getConfigValue("more.prop").getConfigSourceOrdinal());
    }

    @Test
    void warningConfigLocationsNotFound() {
        new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .withSources(config(SMALLRYE_CONFIG_LOCATIONS, "not.found"))
                .build();

        assertEquals("SRCFG01005: Could not find sources with smallrye.config.locations in not.found",
                logCapture.records().get(0).getMessage());
    }

    @Test
    void warningConfigLocationsNotFoundFromExisting() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .withSources(config(SMALLRYE_CONFIG_LOCATIONS, "more.properties", "config_ordinal", "1000"))
                .build();

        assertEquals("5678", config.getConfigValue("more.prop").getValue());
        assertTrue(logCapture.records().isEmpty());

        SmallRyeConfigBuilder builder = new SmallRyeConfigBuilder().addDefaultInterceptors();
        for (ConfigSource configSource : config.getConfigSources()) {
            builder.withSources(configSource);
        }
        builder.build();
        assertTrue(logCapture.records().isEmpty());
    }

    @Test
    void profileSourcesInContext(@TempDir Path tempDir) throws Exception {
        Properties mainProperties = new Properties();
        mainProperties.setProperty("config_ordinal", "150");
        mainProperties.setProperty("my.prop.main", "main");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config.properties").toFile())) {
            mainProperties.store(out, null);
        }

        Properties devProperties = new Properties();
        devProperties.setProperty("my.prop.dev", "dev");
        try (FileOutputStream out = new FileOutputStream(tempDir.resolve("config-dev.properties").toFile())) {
            devProperties.store(out, null);
        }

        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .addDefaultSources()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withProfile("dev")
                .withSources(new ConfigSourceProvider() {
                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ClassLoader forClassLoader) {
                        AbstractLocationConfigSourceLoader configSourceLoader = new AbstractLocationConfigSourceLoader() {
                            @Override
                            protected String[] getFileExtensions() {
                                return new String[] { "properties" };
                            }

                            @Override
                            protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
                                return new PropertiesConfigSource(url, ordinal);
                            }
                        };

                        return configSourceLoader.loadConfigSources(tempDir.resolve("config.properties").toUri().toString(),
                                250);
                    }
                })
                .withSources(new ConfigSourceFactory() {
                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                        Map<String, String> values = new HashMap<>();
                        values.put("context.main", context.getValue("my.prop.main").getRawValue());
                        values.put("context.dev", context.getValue("my.prop.dev").getRawValue());
                        return Collections.singletonList(new MapBackedConfigSource("map", values) {
                        });
                    }
                })
                .build();

        assertEquals("main", config.getRawValue("my.prop.main"));
        assertEquals("dev", config.getRawValue("my.prop.dev"));
        assertEquals("main", config.getRawValue("context.main"));
        assertEquals("dev", config.getRawValue("context.dev"));
    }

    private static SmallRyeConfig buildConfig(String... locations) {
        return new SmallRyeConfigBuilder()
                .addDiscoveredSources()
                .addDefaultInterceptors()
                .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, String.join(",", locations))
                .build();
    }

    private static int countSources(SmallRyeConfig config) {
        return (int) stream(config.getConfigSources().spliterator(), false).filter(PropertiesConfigSource.class::isInstance)
                .count();
    }
}
