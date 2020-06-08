package io.smallrye.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class ConfigValuePropertiesTest {
    @Test
    public void singleLine() throws Exception {
        final ConfigValueProperties map = new ConfigValueProperties("config", 1);
        map.load(new StringReader("key=value"));

        assertEquals(1, map.get("key").getLineNumber());
    }

    @Test
    public void multipleLines() throws Exception {
        final ConfigValueProperties map = new ConfigValueProperties("config", 1);
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
                        "key20=value"));

        assertEquals(1, map.get("key").getLineNumber());
        assertEquals(2, map.get("key2").getLineNumber());
        assertEquals(3, map.get("key3").getLineNumber());
        assertEquals(20, map.get("key20").getLineNumber());
    }

    @Test
    public void comments() throws Exception {
        final ConfigValueProperties map = new ConfigValueProperties("config", 1);
        map.load(new StringReader(
                "key=value\n" +
                        "key2=value\n" +
                        "#comment\n" +
                        "#comment\n" +
                        "#comment\n" +
                        "key3=value"));

        assertEquals(1, map.get("key").getLineNumber());
        assertEquals(2, map.get("key2").getLineNumber());
        assertEquals(6, map.get("key3").getLineNumber());
    }

    @Test
    public void wrapValue() throws Exception {
        final ConfigValueProperties map = new ConfigValueProperties("config", 1);
        map.load(new StringReader(
                "key=value\\wrap\n" +
                        "key2=value\\\rwrap\n" +
                        "#comment\f\t\n" +
                        "#comment\r\n" +
                        "\\key3=value"));

        assertEquals(1, map.get("key").getLineNumber());
        assertEquals(2, map.get("key2").getLineNumber());
        assertEquals("valuewrap", map.get("key2").getValue());
        assertEquals(7, map.get("key3").getLineNumber());
    }
}
