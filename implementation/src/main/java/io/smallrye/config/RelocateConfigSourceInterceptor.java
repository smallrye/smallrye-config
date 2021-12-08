package io.smallrye.config;

import java.util.Map;
import java.util.function.Function;

import jakarta.annotation.Priority;

@Priority(Priorities.LIBRARY + 1000)
public class RelocateConfigSourceInterceptor extends AbstractMappingConfigSourceInterceptor {
    private static final long serialVersionUID = 3476637906383945843L;

    public RelocateConfigSourceInterceptor(final Function<String, String> mapping) {
        super(mapping);
    }

    public RelocateConfigSourceInterceptor(final Map<String, String> mappings) {
        super(mappings);
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final String map = getMapping().apply(name);
        ConfigValue configValue = context.proceed(map);
        if (configValue == null && !name.equals(map)) {
            configValue = context.proceed(name);
        }
        return configValue;
    }
}
