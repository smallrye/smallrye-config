package io.smallrye.config.source.yaml;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertNotNull(src.getValue("de.javahippie.mpadmin.instances"));
    }
}
