package io.smallrye.config.cdi.providers;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Making the Config sources available via CDI
 * 
 * @author <a href="mailto:phillip.kruger@redhat.com">Phillip Kruger</a>
 */
@ApplicationScoped
public class ConfigSourceMapProvider {

    @Inject
    private Provider<Config> configProvider;

    private final Map<String, ConfigSource> configSourceMap = new HashMap<>();

    @Produces
    @ConfigSourceMap
    public Map<String, ConfigSource> produceConfigSourceMap() {
        if (this.configSourceMap.isEmpty()) {
            for (ConfigSource configSource : configProvider.get().getConfigSources()) {
                this.configSourceMap.put(configSource.getName(), configSource);
            }
        }
        return this.configSourceMap;
    }
}
