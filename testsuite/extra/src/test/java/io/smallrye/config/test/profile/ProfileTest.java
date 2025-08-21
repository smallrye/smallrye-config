package io.smallrye.config.test.profile;

import static io.smallrye.config.SmallRyeConfig.SMALLRYE_CONFIG_LOCATIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ProfileTest {
    @Test
    void profilesClasspath(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-one.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/microprofile-config.properties")
                .addAsResource(new StringAsset(
                        "my.prop.common=common\n" +
                                "my.prop.profile=common\n"),
                        "META-INF/microprofile-config-common.properties")
                .addAsResource(new StringAsset(
                        "my.prop.dev=dev\n" +
                                "my.prop.profile=dev\n"),
                        "META-INF/microprofile-config-dev.properties");

        Path filePathOne = tempDir.resolve("resources-one.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-two.jar")
                .addAsResource(new StringAsset(
                        "config_ordinal=150\n" +
                                "my.prop.main=main\n" +
                                "my.prop.common=main\n" +
                                "my.prop.profile=main\n"),
                        "META-INF/microprofile-config.properties");

        Path filePathTwo = tempDir.resolve("resources-two.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {
                new URL("jar:" + filePathOne.toUri() + "!/"),
                new URL("jar:" + filePathTwo.toUri() + "!/"),
        }, contextClassLoader)) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDefaultSources()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withProfile("common,dev")
                    .build();

            assertEquals("main", config.getConfigValue("my.prop.main").getValue());
            assertEquals("common", config.getConfigValue("my.prop.common").getValue());
            assertEquals("dev", config.getConfigValue("my.prop.profile").getValue());
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Test
    void profileValueSameOrdinalDifferentSources(@TempDir Path tempDir) throws Exception {
        JavaArchive jarOne = ShrinkWrap
                .create(JavaArchive.class, "resources-child.jar")
                .addAsResource(new StringAsset("my.prop=child\n"), "resources.properties");

        Path filePathOne = tempDir.resolve("resources-child.jar");
        jarOne.as(ZipExporter.class).exportTo(filePathOne.toFile());

        JavaArchive jarTwo = ShrinkWrap
                .create(JavaArchive.class, "resources-parent.jar")
                .addAsResource(new StringAsset("%dev.my.prop=parent\n"), "resources.properties");

        Path filePathTwo = tempDir.resolve("resources-parent.jar");
        jarTwo.as(ZipExporter.class).exportTo(filePathTwo.toFile());

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader urlClassLoader = urlClassLoader(contextClassLoader, "jar:" + filePathOne.toUri() + "!/",
                "jar:" + filePathTwo.toUri() + "!/")) {
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            SmallRyeConfig config = new SmallRyeConfigBuilder()
                    .addDiscoveredSources()
                    .addDefaultInterceptors()
                    .withDefaultValue(SMALLRYE_CONFIG_LOCATIONS, "resources.properties")
                    .withProfile("dev")
                    .build();

            int index = 0;
            int childSourceIndex = -1;
            int parentSourceIndex = -1;
            ConfigSource childSource = null;
            ConfigSource parentSource = null;
            for (ConfigSource source : config.getConfigSources()) {
                if (source.getName().contains("child")) {
                    childSourceIndex = index;
                    childSource = source;
                }
                if (source.getName().contains("parent")) {
                    parentSourceIndex = index;
                    parentSource = source;
                }
                index++;
            }

            assertNotNull(childSource);
            assertNotNull(parentSource);
            assertTrue(childSourceIndex < parentSourceIndex);
            assertEquals(childSource.getOrdinal(), parentSource.getOrdinal());

            assertEquals("dev", config.getProfiles().get(0));
            assertEquals("child", config.getConfigValue("my.prop").getValue());

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
