package io.smallrye.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

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

    private static Config buildConfig(String... keyValues) {
        return SmallRyeConfigProviderResolver.INSTANCE.getBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

}
