package io.smallrye.config.source.hocon;

import java.io.IOException;
import java.net.URL;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.AbstractLocationConfigSourceFactory;

public class HoconConfigSourceFactory extends AbstractLocationConfigSourceFactory {
    @Override
    protected String[] getFileExtensions() {
        return new String[] {
                "conf"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new HoconConfigSource(url, ordinal);
    }
}
