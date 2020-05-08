package io.smallrye.config;

import java.util.Map;
import java.util.function.Function;

import javax.annotation.Priority;

@Priority(Priorities.LIBRARY + 1000)
public class RelocateConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 3476637906383945843L;

    private final Function<String, String> mapping;

    public RelocateConfigSourceInterceptor(final Function<String, String> mapping) {
        this.mapping = mapping != null ? mapping : Function.identity();
    }

    public RelocateConfigSourceInterceptor(final Map<String, String> mappings) {
        this(name -> mappings.getOrDefault(name, name));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final String map = mapping.apply(name);
        ConfigValue configValue = context.proceed(map);
        if (configValue == null && !name.equals(map)) {
            configValue = context.proceed(name);
        }
        return configValue;
    }
}
