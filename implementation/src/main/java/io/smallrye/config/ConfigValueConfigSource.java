package io.smallrye.config;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Extends the original {@link ConfigSource} to expose methods that return a {@link ConfigValue}. The
 * {@link ConfigValue} allows retrieving additional metadata associated with the configuration resolution.
 * <p>
 * This works around the MicroProfile Config {@link ConfigSource} limitations, which exposes everything as plain
 * Strings, and retrieving additional information associated with the Configuration is impossible. The
 * {@link ConfigValueConfigSource} tries to make this possible.
 */
public interface ConfigValueConfigSource extends ConfigSource {
    /**
     * Return the {@link ConfigValue} for the specified property in this configuration source.
     *
     * @param propertyName the property name
     * @return the ConfigValue, or {@code null} if the property is not present
     */
    ConfigValue getConfigValue(String propertyName);

    /**
     * Return the properties in this configuration source as a Map of String and {@link ConfigValue}.
     *
     * @return a map containing properties of this configuration source
     */
    Map<String, ConfigValue> getConfigValueProperties();

    /**
     * Return the properties in this configuration source as a map.
     * <p>
     *
     * This wraps the original {@link ConfigValue} map returned by
     * {@link ConfigValueConfigSource#getConfigValueProperties()} and provides a view over the original map
     * via {@link ConfigValueMapView}.
     *
     * @return a map containing properties of this configuration source
     */
    @Override
    default Map<String, String> getProperties() {
        return Collections.unmodifiableMap(new ConfigValueMapView(getConfigValueProperties()));
    }

    /**
     * Return the value for the specified property in this configuration source.
     * <p>
     *
     * This wraps the original {@link ConfigValue} returned by {@link ConfigValueConfigSource#getConfigValue(String)}
     * and unwraps the property value contained {@link ConfigValue}. If the {@link ConfigValue} is null the unwrapped
     * value and return is also null.
     *
     * @param propertyName the property name
     * @return the property value, or {@code null} if the property is not present
     */
    @Override
    default String getValue(String propertyName) {
        final ConfigValue value = getConfigValue(propertyName);
        return value != null ? value.getValue() : null;
    }

    /**
     * The {@link ConfigValueMapView} is a view over a Map of String configs names and {@link ConfigValue} values.
     * <p>
     *
     * Use it to wrap a Map of {@link ConfigValue} and expose it where a Map of String name and a String value is
     * required.
     */
    final class ConfigValueMapView extends AbstractMap<String, String> {
        private final Map<String, ConfigValue> delegate;

        ConfigValueMapView(final Map<String, ConfigValue> delegate) {
            this.delegate = Collections.unmodifiableMap(delegate);
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
            return values().contains(value);
        }

        @Override
        public String get(final Object key) {
            final ConfigValue configValue = delegate.get(key);
            return configValue != null ? configValue.getValue() : null;
        }

        private transient Set<Entry<String, String>> entrySet;
        private transient Collection<String> values;

        @Override
        public Set<String> keySet() {
            return delegate.keySet();
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            if (entrySet == null) {
                entrySet = new AbstractSet<Entry<String, String>>() {
                    @Override
                    public Iterator<Entry<String, String>> iterator() {
                        return new Iterator<Entry<String, String>>() {
                            final Iterator<Entry<String, ConfigValue>> delegate = ConfigValueMapView.this.delegate.entrySet()
                                    .iterator();

                            @Override
                            public boolean hasNext() {
                                return delegate.hasNext();
                            }

                            @Override
                            public Entry<String, String> next() {
                                final Entry<String, ConfigValue> next = delegate.next();
                                final ConfigValue configValue = next.getValue();
                                final String value = configValue != null ? configValue.getValue() : null;
                                return new SimpleImmutableEntry<>(next.getKey(), value);
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
        public Collection<String> values() {
            if (values == null) {
                values = new AbstractCollection<String>() {
                    @Override
                    public Iterator<String> iterator() {
                        final Iterator<ConfigValue> delegate = ConfigValueMapView.this.delegate.values().iterator();

                        return new Iterator<String>() {
                            @Override
                            public boolean hasNext() {
                                return delegate.hasNext();
                            }

                            @Override
                            public String next() {
                                final ConfigValue configValue = delegate.next();
                                return configValue != null ? configValue.getValue() : null;
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
    }

    /**
     * The {@link ConfigValueMapStringView} is a view over a Map of String configs names and String values.
     * <p>
     *
     * Use it to wrap a Map of Strings and expose it where a Map of String name and a {@link ConfigValue} value is
     * required.
     */
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

        private Set<Entry<String, ConfigValue>> entrySet;
        private Collection<ConfigValue> values;

        @Override
        public Set<Entry<String, ConfigValue>> entrySet() {
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
                        final Iterator<Entry<String, ConfigValue>> delegate = ConfigValueMapStringView.this.entrySet()
                                .iterator();

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
}
