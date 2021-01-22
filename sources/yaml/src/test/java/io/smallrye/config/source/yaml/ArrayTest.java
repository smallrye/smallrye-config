package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

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
}
