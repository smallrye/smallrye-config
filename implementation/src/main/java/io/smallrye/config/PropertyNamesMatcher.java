package io.smallrye.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A matcher of configuration names.
 * <p>
 * Names without wildcards are stored in a {@link HashMap} for exact lookup. Names containing {@code *} or
 * {@code **} are stored in a trie of {@link Node} instances, where each node represents a segment separated by
 * {@code .}. Single {@code *} segments are stored in the {@link Node#wildcard}, and {@code **} sets the
 * {@link Node#greedy} flag to match any number of remaining segments.
 * <p>
 * Lookups ({@link #matches} and {@link #get}) first check the exact-match map, then traverse the wildcard trie,
 * trying named children before falling back to wildcard nodes at each level.
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
        return properties.isEmpty() && wildcards.children == null && wildcards.wildcard == null && !wildcards.greedy;
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

        if (current.greedy) {
            return true;
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
            if (current.wildcard.wildcard == null && current.wildcard.children == null
                    && !current.wildcard.greedy) {
                return true;
            }
            ni.setPosition(position);
            ni.next();
            if (matches(current.wildcard, ni)) {
                return true;
            }
            if (current.wildcard.terminal) {
                return true;
            }
        }

        return false;
    }

    // Sentinel to distinguish "matched with null value" from "no match" in the recursive get traversal
    private static final Object NO_MATCH = new Object();

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

        if (properties.containsKey(name)) {
            return null;
        }

        NameIterator ni = new NameIterator(name);
        result = get(wildcards, ni);
        return result == noMatch() ? null : result;
    }

    private T get(final Node<T> current, final NameIterator ni) {
        if (!ni.hasNext()) {
            return current.terminal ? current.value : noMatch();
        }

        int position = ni.getPosition();
        Node<T> child = current.find(ni);
        if (child != null) {
            ni.next();
            T result = get(child, ni);
            if (result != noMatch()) {
                return result;
            }
        }

        if (current.wildcard != null) {
            if (current.wildcard.wildcard == null && current.wildcard.children == null
                    && !current.wildcard.greedy && !current.greedy) {
                return current.wildcard.value;
            }
            ni.setPosition(position);
            ni.next();
            T result = get(current.wildcard, ni);
            if (result != noMatch()) {
                return result;
            }
            if (current.wildcard.terminal && !current.greedy) {
                return current.wildcard.value;
            }
        }

        if (current.greedy) {
            return current.value;
        }

        return noMatch();
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
                if (ni.nextSegmentEquals("**")) {
                    if (!current.greedy) {
                        current.greedy = true;
                        current.value = value;
                    }
                    return;
                }
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
                if (ni.nextSegmentEquals("**")) {
                    current.greedy = true;
                    current.value = value;
                    return;
                }
                current = current.findOrCreate(ni);
                ni.next();
            }
            current.value = value;
            current.terminal = true;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T noMatch() {
        return (T) NO_MATCH;
    }

    public static final class Node<T> {
        String path;
        T value;
        Node<T>[] children;
        Node<T> wildcard;
        boolean terminal;
        boolean greedy;

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
            // A literal * segment in the input must only be handled by the wildcard node, not matched
            // against named children via PropertyName.equals (which treats * as a wildcard bidirectionally)
            if (len == 1 && ni.getName().charAt(offset) == '*') {
                return null;
            }
            for (Node<T> child : children) {
                if (PropertyName.equals(child.path, 0, child.path.length(), ni.getName(), offset, len)) {
                    return child;
                }
            }
            return null;
        }
    }
}
