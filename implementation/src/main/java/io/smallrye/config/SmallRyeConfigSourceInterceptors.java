package io.smallrye.config;

import java.util.Iterator;
import java.util.List;

public class SmallRyeConfigSourceInterceptors implements ConfigSourceInterceptorContext {
    private final List<ConfigSourceInterceptor> interceptors;

    private int current = 0;

    public SmallRyeConfigSourceInterceptors(final List<ConfigSourceInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    ConfigValue getValue(final String name) {
        ConfigValue configValue = null;
        for (int i = 0; i < interceptors.size(); i++) {
            ConfigSourceInterceptor interceptor = interceptors.get(i);
            configValue = interceptor.getValue(this, name);
        }
        return configValue;
    }

    @Override
    public ConfigValue proceed(final String name) {
        ConfigSourceInterceptorContext context = new ConfigSourceInterceptorContext() {
            int position = 0;

            @Override
            public ConfigValue proceed(final String name) {
                return interceptors.get(position++).getValue(this, name);
            }

            @Override
            public Iterator<String> iterateNames() {
                return null;
            }

            @Override
            public Iterator<ConfigValue> iterateValues() {
                return null;
            }
        };

        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames() {
        return null;
    }

    @Override
    public Iterator<ConfigValue> iterateValues() {
        return null;
    }

    public static void main(String[] args) {
        SmallRyeConfigSourceInterceptors interceptors = new SmallRyeConfigSourceInterceptors(
                List.of(new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }
                }, new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        return context.proceed(name);
                    }
                }, new ConfigSourceInterceptor() {
                    @Override
                    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
                        throw new RuntimeException();
                    }
                }));

        interceptors.getValue("foo.bar");
    }
}
