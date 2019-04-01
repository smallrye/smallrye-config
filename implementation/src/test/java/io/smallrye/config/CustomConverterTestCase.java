package io.smallrye.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.eclipse.microprofile.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class CustomConverterTestCase {

    @Test
    public void testCustomInetAddressConverter() {
        Config config = buildConfig(
                "my.address", "10.0.0.1");
        InetAddress inetaddress = config.getValue("my.address", InetAddress.class);
        assertNotNull(inetaddress);
        assertArrayEquals(new byte[]{10, 0, 0, 1}, inetaddress.getAddress());
    }

    private static Config buildConfig(String... keyValues) {
        return SmallRyeConfigProviderResolver.INSTANCE.getBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .build();
    }

}
