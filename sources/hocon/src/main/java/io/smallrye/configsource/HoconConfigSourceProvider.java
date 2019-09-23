package io.smallrye.configsource;

import java.util.*;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;

public class HoconConfigSourceProvider implements ConfigSourceProvider {

    private static final String META_INF_MICROPROFILE_CONFIG_RESOURCE = "META-INF/microprofile-config.conf";
    private static final String WEB_INF_MICROPROFILE_CONFIG_RESOURCE = "WEB-INF/classes/META-INF/microprofile-config.conf";

    static ConfigSource getConfigSource(ClassLoader classLoader, String resource, int ordinal) {
        final Config config = ConfigFactory.parseResourcesAnySyntax(classLoader, resource,
                ConfigParseOptions.defaults().setClassLoader(classLoader).setSyntax(ConfigSyntax.CONF));
        return new HoconConfigSource(config, resource, ordinal);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
        final List<ConfigSource> configSources = new ArrayList<>(2);
        configSources.add(getConfigSource(classLoader, META_INF_MICROPROFILE_CONFIG_RESOURCE, 60));
        configSources.add(getConfigSource(classLoader, WEB_INF_MICROPROFILE_CONFIG_RESOURCE, 50));
        return Collections.unmodifiableList(configSources);
    }
}
