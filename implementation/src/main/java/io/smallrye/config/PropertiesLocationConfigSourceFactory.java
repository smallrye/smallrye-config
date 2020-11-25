package io.smallrye.config;

import java.io.IOException;
import java.net.URL;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class PropertiesLocationConfigSourceFactory extends AbstractLocationConfigSourceFactory {
    @Override
    public String[] getFileExtensions() {
        return new String[] { "properties" };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new PropertiesConfigSource(url, ordinal);
    }
}
