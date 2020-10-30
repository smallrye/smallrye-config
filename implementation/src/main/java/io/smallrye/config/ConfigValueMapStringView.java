package io.smallrye.config;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

final class ConfigValueMapStringView extends AbstractMap<String, ConfigValue> {
    private final Map<String, String> delegate;
    private final String configSourceName;
    private final int configSourceOrdinal;

    public ConfigValueMapStringView(final Map<String, String> delegate, final String configSourceName,
            final int configSourceOrdinal) {
        this.delegate = Collections.unmodifiableMap(delegate);
        this.configSourceName = configSourceName;
        this.configSourceOrdinal = configSourceOrdinal;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        ConfigValue configValue = (ConfigValue) value;
        if (configValue == null || configValue.getValue() == null) {
            return delegate.containsValue(null);
        }
        return delegate.containsValue(configValue.getValue());
    }

    @Override
    public ConfigValue get(final Object key) {
        final String value = delegate.get(key);
        if (value == null) {
            return null;
        }

        return toConfigValue((String) key, value);
    }

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    private Set<Map.Entry<String, ConfigValue>> entrySet;
    private Collection<ConfigValue> values;

    @Override
    public Set<Map.Entry<String, ConfigValue>> entrySet() {
        if (entrySet == null) {
            entrySet = new AbstractSet<Entry<String, ConfigValue>>() {
                @Override
                public Iterator<Entry<String, ConfigValue>> iterator() {
                    return new Iterator<Entry<String, ConfigValue>>() {
                        final Iterator<Entry<String, String>> delegate = ConfigValueMapStringView.this.delegate.entrySet()
                                .iterator();

                        @Override
                        public boolean hasNext() {
                            return delegate.hasNext();
                        }

                        @Override
                        public Entry<String, ConfigValue> next() {
                            final Entry<String, String> next = delegate.next();
                            final String value = next.getValue();
                            return value != null
                                    ? new SimpleImmutableEntry<>(next.getKey(), toConfigValue(next.getKey(), value))
                                    : new SimpleImmutableEntry<>(next.getKey(), null);
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
        return entrySet;
    }

    @Override
    public Collection<ConfigValue> values() {
        if (values == null) {
            values = new AbstractCollection<ConfigValue>() {
                @Override
                public Iterator<ConfigValue> iterator() {
                    final Iterator<Entry<String, ConfigValue>> delegate = ConfigValueMapStringView.this.entrySet().iterator();

                    return new Iterator<ConfigValue>() {
                        @Override
                        public boolean hasNext() {
                            return delegate.hasNext();
                        }

                        @Override
                        public ConfigValue next() {
                            final Entry<String, ConfigValue> next = delegate.next();
                            return next != null ? next.getValue() : null;
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
        return values;

    }

    private ConfigValue toConfigValue(final String name, final String value) {
        return ConfigValue.builder()
                .withName(name)
                .withValue(value)
                .withRawValue(value)
                .withConfigSourceName(configSourceName)
                .withConfigSourceOrdinal(configSourceOrdinal)
                .build();
    }
}
