package io.smallrye.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import io.smallrye.config._private.ConfigMessages;

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

    /**
     * Loads properties as {@link ConfigValue}.
     * <p>
     *
     * This class is mostly a subset copy of {@link java.util.Properties}. This was required to be able to keep track of
     * the line number from which the configuration was loaded.
     */
    final class ConfigValueProperties extends HashMap<String, ConfigValue> {
        private static final long serialVersionUID = 613423366086278005L;
        private final String configSourceName;
        private final int configSourceOrdinal;

        public ConfigValueProperties(final String configSourceName, final int configSourceOrdinal) {
            this.configSourceName = configSourceName;
            this.configSourceOrdinal = configSourceOrdinal;
        }

        public synchronized void load(Reader reader) throws IOException {
            load0(new LineReader(reader));
        }

        public synchronized void load(InputStream inStream) throws IOException {
            load0(new LineReader(inStream));
        }

        private void load0(LineReader lr) throws IOException {
            char[] convtBuf = new char[1024];
            int limit;
            int keyLen;
            int valueStart;
            char c;
            boolean hasSep;
            boolean precedingBackslash;

            while ((limit = lr.readLine()) >= 0) {
                c = 0;
                keyLen = 0;
                valueStart = limit;
                hasSep = false;

                //System.out.println("line=<" + new String(lineBuf, 0, limit) + ">");
                precedingBackslash = false;
                while (keyLen < limit) {
                    c = lr.lineBuf[keyLen];
                    //need check if escaped.
                    if ((c == '=' || c == ':') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        hasSep = true;
                        break;
                    } else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
                        valueStart = keyLen + 1;
                        break;
                    }
                    if (c == '\\') {
                        precedingBackslash = !precedingBackslash;
                    } else {
                        precedingBackslash = false;
                    }
                    keyLen++;
                }
                while (valueStart < limit) {
                    c = lr.lineBuf[valueStart];
                    if (c != ' ' && c != '\t' && c != '\f') {
                        if (!hasSep && (c == '=' || c == ':')) {
                            hasSep = true;
                        } else {
                            break;
                        }
                    }
                    valueStart++;
                }
                String key = loadConvert(lr.lineBuf, 0, keyLen, convtBuf);
                String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convtBuf);
                put(key, ConfigValue.builder()
                        .withName(key)
                        .withValue(value)
                        .withRawValue(value)
                        .withConfigSourceName(configSourceName)
                        .withConfigSourceOrdinal(configSourceOrdinal)
                        .withLineNumber(lr.lineNumber)
                        .build());
            }
        }

        class LineReader {
            public LineReader(InputStream inStream) {
                this.inStream = inStream;
                inByteBuf = new byte[8192];
            }

            public LineReader(Reader reader) {
                this.reader = reader;
                inCharBuf = new char[8192];
            }

            byte[] inByteBuf;
            char[] inCharBuf;
            char[] lineBuf = new char[1024];
            int inLimit = 0;
            int inOff = 0;
            InputStream inStream;
            Reader reader;
            int lineNumber = 0;
            int addBackslash = 0;

            int readLine() throws IOException {
                int len = 0;
                char c = 0;

                boolean skipWhiteSpace = true;
                boolean isCommentLine = false;
                boolean isNewLine = true;
                boolean appendedLineBegin = false;
                boolean precedingBackslash = false;
                boolean skipLF = false;
                lineNumber = ++lineNumber + addBackslash;
                addBackslash = 0;

                while (true) {
                    if (inOff >= inLimit) {
                        inLimit = (inStream == null) ? reader.read(inCharBuf)
                                : inStream.read(inByteBuf);
                        inOff = 0;
                        if (inLimit <= 0) {
                            if (len == 0 || isCommentLine) {
                                return -1;
                            }
                            if (precedingBackslash) {
                                len--;
                            }
                            return len;
                        }
                    }
                    if (inStream != null) {
                        //The line below is equivalent to calling a
                        //ISO8859-1 decoder.
                        c = (char) (0xff & inByteBuf[inOff++]);
                    } else {
                        c = inCharBuf[inOff++];
                    }
                    if (skipLF) {
                        skipLF = false;
                        if (c == '\n') {
                            continue;
                        }
                    }
                    if (skipWhiteSpace) {
                        if (c == ' ' || c == '\t' || c == '\f') {
                            continue;
                        }
                        if (!appendedLineBegin && (c == '\r' || c == '\n')) {
                            if (c == '\n') {
                                lineNumber++;
                            }
                            continue;
                        }
                        skipWhiteSpace = false;
                        appendedLineBegin = false;
                    }
                    if (isNewLine) {
                        isNewLine = false;
                        if (c == '#' || c == '!') {
                            isCommentLine = true;
                            continue;
                        }
                    }

                    if (c != '\n' && c != '\r') {
                        lineBuf[len++] = c;
                        if (len == lineBuf.length) {
                            int newLength = lineBuf.length * 2;
                            if (newLength < 0) {
                                newLength = Integer.MAX_VALUE;
                            }
                            char[] buf = new char[newLength];
                            System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
                            lineBuf = buf;
                        }
                        //flip the preceding backslash flag
                        if (c == '\\') {
                            precedingBackslash = !precedingBackslash;
                        } else {
                            precedingBackslash = false;
                        }
                    } else {
                        // reached EOL
                        if (isCommentLine || len == 0) {
                            isCommentLine = false;
                            isNewLine = true;
                            skipWhiteSpace = true;
                            len = 0;
                            lineNumber++;
                            continue;
                        }
                        if (inOff >= inLimit) {
                            inLimit = (inStream == null)
                                    ? reader.read(inCharBuf)
                                    : inStream.read(inByteBuf);
                            inOff = 0;
                            if (inLimit <= 0) {
                                if (precedingBackslash) {
                                    len--;
                                }
                                return len;
                            }
                        }
                        if (precedingBackslash) {
                            len -= 1;
                            //skip the leading whitespace characters in following line
                            skipWhiteSpace = true;
                            appendedLineBegin = true;
                            precedingBackslash = false;
                            addBackslash++;
                            if (c == '\r') {
                                skipLF = true;
                            }
                        } else {
                            if (c == '\r') {
                                inOff++;
                            }

                            return len;
                        }
                    }
                }
            }
        }

        private String loadConvert(char[] in, int off, int len, char[] convtBuf) {
            if (convtBuf.length < len) {
                int newLen = len * 2;
                if (newLen < 0) {
                    newLen = Integer.MAX_VALUE;
                }
                convtBuf = new char[newLen];
            }
            char aChar;
            char[] out = convtBuf;
            int outLen = 0;
            int end = off + len;

            while (off < end) {
                aChar = in[off++];
                if (aChar == '\\') {
                    aChar = in[off++];
                    if (aChar == 'u') {
                        // Read the xxxx
                        int value = 0;
                        for (int i = 0; i < 4; i++) {
                            aChar = in[off++];
                            switch (aChar) {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + aChar - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + aChar - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + aChar - 'A';
                                    break;
                                default:
                                    throw ConfigMessages.msg.malformedEncoding();
                            }
                        }
                        out[outLen++] = (char) value;
                    } else {
                        if (aChar == 't') {
                            aChar = '\t';
                        } else if (aChar == 'r') {
                            aChar = '\r';
                        } else if (aChar == 'n') {
                            aChar = '\n';
                        } else if (aChar == 'f') {
                            aChar = '\f';
                        }
                        out[outLen++] = aChar;
                    }
                } else {
                    out[outLen++] = aChar;
                }
            }
            return new String(out, 0, outLen);
        }
    }
}
