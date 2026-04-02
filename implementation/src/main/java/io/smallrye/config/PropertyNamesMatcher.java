package io.smallrye.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A matcher of configuration names.
 * <p>
 * Due to the equality rules of {@link PropertyName}, it is unsuitable to be used as is in hash-based
 * searches. Instead, this matcher splits the names by prefixes, which act as buckets, and then does a linear search.
 */
public class PropertyNamesMatcher<T> {
    private final Map<String, T> properties = new HashMap<>();
    private final Node<T> wildcards = new Node<>();

    protected PropertyNamesMatcher() {
    }

    protected Map<String, T> getProperties() {
        return properties;
    }

    protected Node<T> getWildcards() {
        return wildcards;
    }

    public boolean isEmpty() {
        return properties.isEmpty() && wildcards.children == null && wildcards.wildcard == null;
    }

    /**
     * Match a name with any of the names contained in this matcher.
     *
     * @param name the String to be matched
     * @return {@code true} if matched or {@code false} otherwise.
     */
    public boolean matches(final String name) {
        boolean match = properties.containsKey(name);
        if (match) {
            return true;
        }

        NameIterator ni = new NameIterator(name);
        return matches(wildcards, ni);
    }

    private boolean matches(final Node<T> current, final NameIterator ni) {
        if (!ni.hasNext()) {
            return current.terminal;
        }

        int position = ni.getPosition();
        Node<T> child = current.find(ni);
        if (child != null) {
            ni.next();
            if (matches(child, ni)) {
                return true;
            }
        }

        if (current.wildcard != null) {
            if (current.wildcard.wildcard == null && current.wildcard.children == null) {
                return true;
            }
            ni.setPosition(position);
            ni.next();
            if (matches(current.wildcard, ni)) {
                return true;
            }
            return current.wildcard.terminal;
        }

        return false;
    }

    /**
     * Returns the value matching a name with any of the names contained in this matcher.
     *
     * @param name the String to be matched
     * @return a {@code T} value if the name matches or {@code null} otherwise.
     */
    public T get(final String name) {
        T result = properties.get(name);
        if (result != null) {
            return result;
        }

        NameIterator ni = new NameIterator(name);
        return get(wildcards, ni);
    }

    private T get(final Node<T> current, final NameIterator ni) {
        if (!ni.hasNext()) {
            return current.value;
        }

        int position = ni.getPosition();
        Node<T> child = current.find(ni);
        if (child != null) {
            ni.next();
            T result = get(child, ni);
            if (result != null) {
                return result;
            }
        }

        if (current.wildcard != null) {
            if (current.wildcard.wildcard == null && current.wildcard.children == null) {
                return current.wildcard.value;
            }
            ni.setPosition(position);
            ni.next();
            T result = get(current.wildcard, ni);
            if (result != null) {
                return result;
            }
            return current.wildcard.terminal ? current.wildcard.value : null;
        }

        return null;
    }

    protected void add(final String name) {
        add(name, null);
    }

    protected void add(final Set<String> names) {
        for (String name : names) {
            add(name);
        }
    }

    protected void add(final Map<String, T> properties) {
        for (Entry<String, T> entry : properties.entrySet()) {
            add(entry.getKey(), entry.getValue());
        }
    }

    protected void add(final String name, final T value) {
        if (name.indexOf('*') == -1) {
            properties.putIfAbsent(name, value);
        } else {
            Node<T> current = wildcards;
            NameIterator ni = new NameIterator(name);
            while (ni.hasNext()) {
                current = current.findOrCreate(ni);
                ni.next();
            }
            // Do not override value if already set
            if (!current.terminal) {
                current.value = value;
                current.terminal = true;
            }
        }
    }

    protected void put(final Map<String, T> properties) {
        for (Entry<String, T> entry : properties.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    protected void put(final String name, final T value) {
        if (name.indexOf('*') == -1) {
            properties.put(name, value);
        } else {
            Node<T> current = wildcards;
            NameIterator ni = new NameIterator(name);
            while (ni.hasNext()) {
                current = current.findOrCreate(ni);
                ni.next();
            }
            current.value = value;
            current.terminal = true;
        }
    }

    public static final class Node<T> {
        String path;
        T value;
        Node<T>[] children;
        Node<T> wildcard;
        boolean terminal;

        @SuppressWarnings("unchecked")
        public Node<T> findOrCreate(NameIterator ni) {
            if (children != null) {
                for (Node<T> child : children) {
                    if (ni.nextSegmentEquals(child.path)) {
                        return child;
                    }
                }
            }

            if (wildcard != null && ni.nextSegmentEquals("*")) {
                return wildcard;
            }

            Node<T> child = new Node<>();
            int offset = ni.getPosition() + 1;
            child.path = ni.getName().substring(offset, ni.getNextEnd());
            if (child.path.equals("*")) {
                wildcard = child;
            } else {
                if (children != null) {
                    children = Arrays.copyOf(children, children.length + 1);
                } else {
                    children = new Node[1];
                }
                children[children.length - 1] = child;
            }
            return child;
        }

        public Node<T> find(NameIterator ni) {
            if (children == null) {
                return null;
            }

            int offset = ni.getPosition() + 1;
            int len = ni.getNextEnd() - offset;
            for (Node<T> child : children) {
                if (PropertyName.equals(child.path, 0, child.path.length(), ni.getName(), offset, len)) {
                    return child;
                }
            }
            return null;
        }
    }
}
