package io.smallrye.config;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.smallrye.common.annotation.Experimental;

/**
 * The ConfigValueMapView is view over a Map of String configs names and ConfigValue value.
 * <p>
 *
 * Use this to wrap the ConfigValue map and expose it where a Map of String name and String value is required.
 */
@Experimental("Extension to the original ConfigSource to allow retrieval of additional metadata on config lookup")
public final class ConfigValueMapView extends AbstractMap<String, String> {
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

    private transient Set<Map.Entry<String, String>> entrySet;
    private transient Collection<String> values;

    @Override
    public Set<String> keySet() {
        return delegate.keySet();
    }

    @Override
    public Set<Map.Entry<String, String>> entrySet() {
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
                            return new AbstractMap.SimpleImmutableEntry<>(next.getKey(), value);
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
