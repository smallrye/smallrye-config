package io.smallrye.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.utils.ConfigSourceUtil;

@Deprecated(forRemoval = true)
public class ConfigValuePropertiesConfigSource extends MapBackedConfigValueConfigSource {
    private static final long serialVersionUID = 9070158352250209380L;

    private static final String NAME_PREFIX = "ConfigValuePropertiesConfigSource[source=";

    public ConfigValuePropertiesConfigSource(URL url) throws IOException {
        this(url, DEFAULT_ORDINAL);
    }

    public ConfigValuePropertiesConfigSource(URL url, int defaultOrdinal) throws IOException {
        this(url, NAME_PREFIX + url.toString() + "]", defaultOrdinal);
    }

    private ConfigValuePropertiesConfigSource(URL url, String name, int defaultOrdinal) throws IOException {
        super(name, urlToConfigValueMap(url, name, defaultOrdinal));
    }

    public ConfigValuePropertiesConfigSource(Map<String, String> properties, String name, int defaultOrdinal) {
        super(NAME_PREFIX + name + "]",
                new ConfigValueMapStringView(properties, name, ConfigSourceUtil.getOrdinalFromMap(properties, defaultOrdinal)),
                defaultOrdinal);
    }

    private static Map<String, ConfigValue> urlToConfigValueMap(URL locationOfProperties, String name, int ordinal)
            throws IOException {
        try (InputStreamReader reader = new InputStreamReader(locationOfProperties.openStream(), StandardCharsets.UTF_8)) {
            ConfigValueProperties p = new ConfigValueProperties(name, ordinal);
            p.load(reader);
            return p;
        }
    }

    /**
     * Loads properties as {@link ConfigValue}.
     * <p>
     *
     * This class is mostly a subset copy of {@link java.util.Properties}. This was required to be able to keep track of
     * the line number from which the configuration was loaded.
     */
    static class ConfigValueProperties extends HashMap<String, ConfigValue> {
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
