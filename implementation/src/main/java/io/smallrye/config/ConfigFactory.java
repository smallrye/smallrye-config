package io.smallrye.config;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Created by bob on 6/26/18.
 *
 * @deprecated This interface should no longer be used.
 */
@Deprecated
public interface ConfigFactory {
    Config newConfig(List<ConfigSource> sources, Map<Type, Converter<?>> configConverters);
}
