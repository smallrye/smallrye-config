package io.smallrye.config.source.yaml;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;

public class ArrayTest {

    @Test
    public void array() {
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
        assertNotNull(src.getValue("de.javahippie.mpadmin.instances"));
    }
}
