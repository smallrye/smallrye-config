package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

class ConfigValuePropertiesTest {
    @Test
    void singleLine() throws Exception {
        final ConfigValuePropertiesConfigSource.ConfigValueProperties map = new ConfigValuePropertiesConfigSource.ConfigValueProperties(
                "config", 1);
        map.load(new StringReader("key=value"));

        assertEquals(1, map.get("key").getLineNumber());
    }

    @Test
    void multipleLines() throws Exception {
        final ConfigValuePropertiesConfigSource.ConfigValueProperties map = new ConfigValuePropertiesConfigSource.ConfigValueProperties(
                "config", 1);
        map.load(new StringReader(
                "key=value\n" +
                        "key2=value\n" +
                        "key3=value\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "key20=value\n"));

        assertEquals(1, map.get("key").getLineNumber());
        assertEquals(2, map.get("key2").getLineNumber());
        assertEquals(3, map.get("key3").getLineNumber());
        assertEquals(20, map.get("key20").getLineNumber());
    }

    @Test
    void comments() throws Exception {
        final ConfigValuePropertiesConfigSource.ConfigValueProperties map = new ConfigValuePropertiesConfigSource.ConfigValueProperties(
                "config", 1);
        map.load(new StringReader(
                "key=value\n" +
                        "key2=value\n" +
                        "#comment\n" +
                        "#comment\n" +
                        "#comment\n" +
                        "key3=value\n"));

        assertEquals(1, map.get("key").getLineNumber());
        assertEquals(2, map.get("key2").getLineNumber());
        assertEquals(6, map.get("key3").getLineNumber());
    }

    @Test
    void wrapValue() throws Exception {
        final ConfigValuePropertiesConfigSource.ConfigValueProperties map = new ConfigValuePropertiesConfigSource.ConfigValueProperties(
                "config", 1);
        map.load(new StringReader(
                "key=value\\wrap\n" +
                        "key2=value\\\nwrap\n" +
                        "#comment\f\t\n" +
                        "#comment\n" +
                        "\\key3=value\n"));

        assertEquals(1, map.get("key").getLineNumber());
        assertEquals("valuewrap", map.get("key2").getValue());
        assertEquals(2, map.get("key2").getLineNumber());
        assertEquals(6, map.get("key3").getLineNumber());
    }
}
