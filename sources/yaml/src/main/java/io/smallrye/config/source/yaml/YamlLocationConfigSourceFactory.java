package io.smallrye.config.source.yaml;

import java.io.IOException;
import java.net.URL;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config.AbstractLocationConfigSourceFactory;

public class YamlLocationConfigSourceFactory extends AbstractLocationConfigSourceFactory {
    @Override
    public String[] getFileExtensions() {
        return new String[] {
                "yaml",
                "yml"
        };
    }

    @Override
    protected ConfigSource loadConfigSource(final URL url, final int ordinal) throws IOException {
        return new YamlConfigSource(url, ordinal);
    }
}
