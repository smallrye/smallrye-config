/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.config;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.common.constraint.Assert;

/**
 * A {@link Map Map&lt;String, String>} which is backed by a {@link ConfigSource}.
 * This should not be used to implement {@link ConfigSource#getProperties()} on {@code ConfigSource}
 * instances which do not override {@code getPropertyNames()}, as this will result in infinite recursion.
 *
 * @implNote The key set of the map is the result of calling {@link ConfigSource#getPropertyNames()}; the rest
 *           of the map operations are derived from this method and {@link ConfigSource#getValue(String)}.
 *           The values collection and entry set are instantiated lazily and cached.
 *           The implementation attempts to make no assumptions about the efficiency of the backing implementation and
 *           prefers the most direct access possible.
 *           <p>
 *           The backing collections are assumed to be immutable.
 */
public class ConfigSourceMap extends AbstractMap<String, String> implements Map<String, String>, Serializable {
    private static final long serialVersionUID = -6694358608066599032L;

    private final ConfigSource delegate;
    private transient Values values;
    private transient EntrySet entrySet;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate configuration source (must not be {@code null})
     */
    public ConfigSourceMap(final ConfigSource delegate) {
        this.delegate = Assert.checkNotNullParam("delegate", delegate);
    }

    public int size() {
        return delegate.getPropertyNames().size();
    }

    public boolean isEmpty() {
        // may be cheaper in some cases
        return delegate.getPropertyNames().isEmpty();
    }

    public boolean containsKey(final Object key) {
        //noinspection SuspiciousMethodCalls - it's OK in this case
        return delegate.getPropertyNames().contains(key);
    }

    public String get(final Object key) {
        return key instanceof String ? delegate.getValue((String) key) : null;
    }

    public Set<String> keySet() {
        return delegate.getPropertyNames();
    }

    public Collection<String> values() {
        Values values = this.values;
        if (values == null)
            return this.values = new Values();
        return values;
    }

    public Set<Entry<String, String>> entrySet() {
        EntrySet entrySet = this.entrySet;
        if (entrySet == null)
            return this.entrySet = new EntrySet();
        return entrySet;
    }

    public void forEach(final BiConsumer<? super String, ? super String> action) {
        // superclass is implemented in terms of entry set - expensive!
        for (String name : keySet()) {
            action.accept(name, delegate.getValue(name));
        }
    }

    final class Values extends AbstractCollection<String> implements Collection<String> {
        public Iterator<String> iterator() {
            return new Itr(delegate.getPropertyNames().iterator());
        }

        public int size() {
            return delegate.getPropertyNames().size();
        }

        public boolean isEmpty() {
            // may be cheaper in some cases
            return delegate.getPropertyNames().isEmpty();
        }

        final class Itr implements Iterator<String> {
            private final Iterator<String> iterator;

            Itr(final Iterator<String> iterator) {
                this.iterator = iterator;
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public String next() {
                return delegate.getValue(iterator.next());
            }
        }
    }

    final class EntrySet extends AbstractSet<Entry<String, String>> {
        public Iterator<Entry<String, String>> iterator() {
            return new Itr(delegate.getPropertyNames().iterator());
        }

        public int size() {
            return delegate.getPropertyNames().size();
        }

        public boolean isEmpty() {
            // may be cheaper in some cases
            return delegate.getPropertyNames().isEmpty();
        }

        final class Itr implements Iterator<Entry<String, String>> {
            private final Iterator<String> iterator;

            Itr(final Iterator<String> iterator) {
                this.iterator = iterator;
            }

            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Entry<String, String> next() {
                String name = iterator.next();
                return new SimpleImmutableEntry<>(name, delegate.getValue(name));
            }
        }
    }
}
