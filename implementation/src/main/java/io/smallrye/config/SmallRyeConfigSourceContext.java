package io.smallrye.config;

import java.util.Iterator;
import java.util.List;

class SmallRyeConfigSourceContext implements ConfigSourceContext {
    private final ConfigSourceInterceptorContext context;
    private final List<String> profiles;

    public SmallRyeConfigSourceContext(final ConfigSourceInterceptorContext context, final List<String> profiles) {
        this.context = context;
        this.profiles = profiles;
    }

    @Override
    public ConfigValue getValue(final String name) {
        ConfigValue value = context.proceed(name);
        return value != null ? value : ConfigValue.builder().withName(name).build();
    }

    @Override
    public List<String> getProfiles() {
        return profiles;
    }

    @Override
    public Iterator<String> iterateNames() {
        return context.iterateNames();
    }
}
