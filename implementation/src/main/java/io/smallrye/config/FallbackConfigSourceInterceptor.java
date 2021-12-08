package io.smallrye.config;

import java.util.Map;
import java.util.function.Function;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 600)
public class FallbackConfigSourceInterceptor extends AbstractMappingConfigSourceInterceptor {
    private static final long serialVersionUID = 1472367702046537565L;

    public FallbackConfigSourceInterceptor(final Function<String, String> mapping) {
        super(mapping);
    }

    public FallbackConfigSourceInterceptor(final Map<String, String> mappings) {
        super(mappings);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);
        if (configValue == null || configValue.getValue().isEmpty()) {
            final String map = getMapping().apply(name);
            if (!name.equals(map)) {
                configValue = context.proceed(map);
            }
        }
        return configValue;
    }
}
