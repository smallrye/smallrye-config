package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.time.LocalDate;

import org.eclipse.microprofile.config.Config;
import org.junit.Test;

public class ImplicitConverterTestCase {

    @Test
    public void testImplicitURLConverter() {
        Config config = buildConfig(
                "my.url", "https://github.com/smallrye/smallrye-config/");
        URL url = config.getValue("my.url", URL.class);
        assertNotNull(url);
        assertEquals("https", url.getProtocol());
        assertEquals("github.com", url.getHost());
        assertEquals("/smallrye/smallrye-config/", url.getPath());
    }

    @Test
    public void testImplicitLocalDateConverter() {
        Config config = buildConfig(
                "my.date", "2019-04-01");
        LocalDate date = config.getValue("my.date", LocalDate.class);
        assertNotNull(date);
        assertEquals(2019, date.getYear());
        assertEquals(4, date.getMonthValue());
        assertEquals(1, date.getDayOfMonth());
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

}
