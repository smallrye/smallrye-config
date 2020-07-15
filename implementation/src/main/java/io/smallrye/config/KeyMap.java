package io.smallrye.config;

import java.io.Serializable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.function.Functions;

/**
 * A multi-level key map.
 */
public final class KeyMap<V> extends HashMap<String, KeyMap<V>> {
    private static final Object NO_VALUE = new Serializable() {
        private static final long serialVersionUID = -6072559389176920349L;
    };

    private static final long serialVersionUID = 3584966224369608557L;

    private KeyMap<V> any;
    @SuppressWarnings("unchecked")
    private V rootValue = (V) NO_VALUE;

    public KeyMap(final int initialCapacity, final float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public KeyMap(final int initialCapacity) {
        super(initialCapacity);
    }

    public KeyMap() {
    }

    public KeyMap<V> get(final String key, int offs, int len) {
        return get(key.substring(offs, offs + len));
    }

    public KeyMap<V> getAny() {
        return any;
    }

    public KeyMap<V> getOrCreateAny() {
        KeyMap<V> any = this.any;
        if (any == null) {
            any = this.any = new KeyMap<>();
        }
        return any;
    }

    public KeyMap<V> putAny(KeyMap<V> any) {
        KeyMap<V> oldAny = this.any;
        this.any = any;
        return oldAny;
    }

    public KeyMap<V> putAnyIfAbsent(KeyMap<V> any) {
        KeyMap<V> oldAny = this.any;
        if (oldAny == null) {
            this.any = any;
            return null;
        } else {
            return oldAny;
        }
    }

    public boolean hasRootValue() {
        return rootValue != NO_VALUE;
    }

    public V getRootValue() {
        return getRootValueOrDefault(null);
    }

    public V getRootValueOrDefault(final V defaultVal) {
        V rootValue = this.rootValue;
        return rootValue == NO_VALUE ? defaultVal : rootValue;
    }

    public V getOrComputeRootValue(final Supplier<V> supplier) {
        V rootValue = this.rootValue;
        if (rootValue == NO_VALUE) {
            this.rootValue = rootValue = supplier.get();
        }
        return rootValue;
    }

    @SuppressWarnings("unchecked")
    public V removeRootValue() {
        V rootValue = this.rootValue;
        if (rootValue != NO_VALUE) {
            this.rootValue = (V) NO_VALUE;
        }
        return rootValue;
    }

    public V putRootValue(final V rootValue) {
        V old = this.rootValue;
        this.rootValue = rootValue;
        return old == NO_VALUE ? null : old;
    }

    public KeyMap<V> find(final String path) {
        return find(new NameIterator(path));
    }

    public KeyMap<V> find(final NameIterator ni) {
        if (!ni.hasNext()) {
            return this;
        }
        String seg = ni.getNextSegment();
        ni.next();
        KeyMap<V> next = getOrDefault(seg, any);
        return next == null ? null : next.find(ni);
    }

    public KeyMap<V> find(final Iterator<String> iter) {
        if (!iter.hasNext()) {
            return this;
        }
        String seg = iter.next();
        KeyMap<V> next = seg.equals("*") ? any : getOrDefault(seg, any);
        return next == null ? null : next.find(iter);
    }

    public KeyMap<V> find(final Iterable<String> i) {
        return find(i.iterator());
    }

    public KeyMap<V> findOrAdd(final String path) {
        return findOrAdd(new NameIterator(path));
    }

    public KeyMap<V> findOrAdd(final NameIterator ni) {
        if (!ni.hasNext()) {
            return this;
        }
        String seg = ni.getNextSegment();
        ni.next();
        try {
            KeyMap<V> next = seg.equals("*") ? getOrCreateAny() : computeIfAbsent(seg, k -> new KeyMap<>());
            return next.findOrAdd(ni);
        } finally {
            ni.previous();
        }
    }

    public KeyMap<V> findOrAdd(final Iterator<String> iter) {
        if (!iter.hasNext()) {
            return this;
        }
        String seg = iter.next();
        KeyMap<V> next = seg.equals("*") ? getOrCreateAny() : computeIfAbsent(seg, k -> new KeyMap<>());
        return next.findOrAdd(iter);
    }

    public KeyMap<V> findOrAdd(final Iterable<String> i) {
        return findOrAdd(i.iterator());
    }

    public KeyMap<V> findOrAdd(final String... keys) {
        return findOrAdd(keys, 0, keys.length);
    }

    public KeyMap<V> findOrAdd(final String[] keys, int off, int len) {
        String seg = keys[off];
        KeyMap<V> next = seg.equals("*") ? getOrCreateAny() : computeIfAbsent(seg, k -> new KeyMap<>());
        return off + 1 > len - 1 ? next : next.findOrAdd(keys, off + 1, len);
    }

    public V findRootValue(final String path) {
        return findRootValue(new NameIterator(path));
    }

    public V findRootValue(final NameIterator ni) {
        KeyMap<V> result = find(ni);
        return result == null ? null : result.getRootValue();
    }

    public boolean hasRootValue(final String path) {
        return hasRootValue(new NameIterator(path));
    }

    public boolean hasRootValue(final NameIterator ni) {
        KeyMap<V> result = find(ni);
        return result != null && result.hasRootValue();
    }

    public <P, V2> KeyMap<V2> map(P param, BiFunction<P, V, V2> conversion) {
        return map(param, conversion, new IdentityHashMap<>());
    }

    public <V2> KeyMap<V2> map(Function<V, V2> conversion) {
        return map(conversion, Functions.functionBiFunction());
    }

    <P, V2> KeyMap<V2> map(P param, BiFunction<P, V, V2> conversion, IdentityHashMap<KeyMap<V>, KeyMap<V2>> cached) {
        if (cached.containsKey(this)) {
            return cached.get(this);
        }
        KeyMap<V2> newMap = new KeyMap<>(size());
        cached.put(this, newMap);
        Set<Entry<String, KeyMap<V>>> entries = entrySet();
        for (Entry<String, KeyMap<V>> entry : entries) {
            newMap.put(entry.getKey(), entry.getValue().map(param, conversion, cached));
        }
        KeyMap<V> any = getAny();
        if (any != null) {
            newMap.putAny(any.map(param, conversion, cached));
        }
        if (hasRootValue()) {
            newMap.putRootValue(conversion.apply(param, getRootValue()));
        }
        return newMap;
    }

    public StringBuilder toString(StringBuilder b) {
        b.append("KeyMap(");
        V rootValue = this.rootValue;
        if (rootValue == NO_VALUE) {
            b.append("no value");
        } else {
            b.append("value=").append(rootValue);
        }
        b.append(") {");
        final Iterator<Entry<String, KeyMap<V>>> iterator = entrySet().iterator();
        KeyMap<V> any = this.any;
        if (iterator.hasNext()) {
            Entry<String, KeyMap<V>> entry = iterator.next();
            b.append(entry.getKey()).append("=>").append(entry.getValue());
            while (iterator.hasNext()) {
                entry = iterator.next();
                b.append(',').append(entry.getKey()).append("=>").append(entry.getValue());
            }
            if (any != null) {
                b.append(',').append("(any)=>").append(any);
            }
        } else {
            if (any != null) {
                b.append("(any)=>").append(any);
            }
        }
        b.append('}');
        return b;
    }

    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public Map<String, V> toMap() {
        final Map<String, V> map = new HashMap<>();
        unwrap(this, "", map);
        return map;
    }

    private void unwrap(KeyMap<V> keyMap, String key, Map<String, V> map) {
        for (String path : keyMap.keySet()) {
            final KeyMap<V> nested = keyMap.get(path);
            final String nestedKey = key.length() == 0 ? path : key + "." + path;
            if (nested.any != null) {
                map.put(nestedKey + ".*", nested.any.getRootValue());
                unwrap(nested.any, nestedKey + ".*", map);
            }
            if (!nested.hasRootValue()) {
                unwrap(nested, nestedKey, map);
            } else {
                map.put(nestedKey, nested.getRootValue());
                if (!nested.keySet().isEmpty()) {
                    unwrap(nested, nestedKey, map);
                }
            }
        }
    }
}
