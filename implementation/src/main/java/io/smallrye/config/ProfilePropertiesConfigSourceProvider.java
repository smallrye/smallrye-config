package io.smallrye.config;

import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class ProfilePropertiesConfigSourceProvider implements ConfigSourceProvider {
    private final List<ConfigSource> configSources;

    public ProfilePropertiesConfigSourceProvider(String propertyFileName, boolean optional, ClassLoader classLoader) {
        final ConfigurableConfigSource configurableConfigSource = new ConfigurableConfigSource(
                new ConfigSourceFactory() {
                    @Override
                    public ConfigSource getConfigSource(final ConfigSourceContext context) {
                        return null;
                    }

                    @Override
                    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
                        final ConfigValue value = context.getValue("smallrye.config.profile");
                        if (value != null) {
                            final String profileName = value.getValue();
                            final String profileFileName = propertyFileName.replaceAll("microprofile-config",
                                    "microprofile-config-" + profileName);
                            final PropertiesConfigSourceProvider propertiesConfigSourceProvider = new PropertiesConfigSourceProvider(
                                    profileFileName, optional, classLoader);
                            return propertiesConfigSourceProvider.getConfigSources(classLoader);
                        }

                        return Collections.emptyList();
                    }

                    @Override
                    public OptionalInt getPriority() {
                        return OptionalInt.of(101);
                    }
                });

        this.configSources = Collections.singletonList(configurableConfigSource);
    }

    @Override
    public List<ConfigSource> getConfigSources(final ClassLoader classLoader) {
        return configSources;
    }
}
