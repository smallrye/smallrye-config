package io.smallrye.config;

import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;

interface ProfileConfigSourceFactory extends ConfigSourceFactory {
    @Override
    default Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        final List<String> profiles = context.getProfiles();
        if (profiles.isEmpty()) {
            return Collections.emptyList();
        }

        return getProfileConfigSources(profiles);
    }

    Iterable<ConfigSource> getProfileConfigSources(final List<String> profiles);
}
