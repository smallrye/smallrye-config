package io.smallrye.config.source.yaml.test;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.source.yaml.YamlConfigSource;

class ArrayTest {
    @Test
    void array() {
        String yaml = "de:\n" +
                "  javahippie:\n" +
                "    mpadmin:\n" +
                "      instances:\n" +
                "        -\n" +
                "          name: \"Bing\"\n" +
                "          uri: \"https://bing.com\"\n" +
                "        -\n" +
                "          name: \"Google\"\n" +
                "          uri: \"https://www.google.com\"";

        ConfigSource src = new YamlConfigSource("Yaml", yaml);
        assertEquals("Bing", src.getValue("de.javahippie.mpadmin.instances[0].name"));
        assertEquals("https://bing.com", src.getValue("de.javahippie.mpadmin.instances[0].uri"));
        assertEquals("Google", src.getValue("de.javahippie.mpadmin.instances[1].name"));
        assertEquals("https://www.google.com", src.getValue("de.javahippie.mpadmin.instances[1].uri"));
    }

    @Test
    void nullValue() {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("Yaml", "foo:\n"
                        + "    - something\n"
                        + "    - 1\n"
                        + "    - true\n"
                        + "    - ~\n"))
                .build();

        assertEquals("something,1,true", config.getConfigValue("foo").getValue());
        assertEquals("something", config.getConfigValue("foo[0]").getValue());
        assertEquals("1", config.getConfigValue("foo[1]").getValue());
        assertEquals("true", config.getConfigValue("foo[2]").getValue());
        assertNull(config.getConfigValue("foo[3]").getValue());
        Set<String> names = stream(config.getPropertyNames().spliterator(), false).collect(toSet());
        assertFalse(names.contains("foo[3]"));
    }
}
