package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

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

        assertEquals("something", config.getRawValue("foo[0]"));
        assertEquals("1", config.getRawValue("foo[1]"));
        assertEquals("true", config.getRawValue("foo[2]"));
        assertNull(config.getRawValue("foo[3]"));
        assertFalse(((Set<String>) config.getPropertyNames()).contains("foo[3]"));
    }
}
