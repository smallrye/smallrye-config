package io.smallrye.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.smallrye.config.common.MapBackedConfigSource;

/**
 * Parses a String array of arguments (-f or -f=foo or --file or --file=foo)
 *
 * @author George Gastaldi <gegastaldi@gmail.com>
 */
public class ArgumentConfigSource extends MapBackedConfigSource {

    static final String TRUE = "true";
    private static final Pattern ARG_MATCHER = Pattern.compile("-{1,2}([\\w\\d-_]+)=?(.*)");

    public ArgumentConfigSource(String[] args) {
        this("ArgumentConfigSource", args, 500);
    }

    public ArgumentConfigSource(String name, String... args) {
        this(name, args, 500);
    }

    public ArgumentConfigSource(String name, String[] args, int ordinal) {
        super(name, toMap(Objects.requireNonNull(args, "args cannot be null")), ordinal);
    }

    private static Map<String, String> toMap(String[] args) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        String lastKey = null;
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                continue;
            }
            Matcher matcher = ARG_MATCHER.matcher(arg);
            if (matcher.matches()) {
                String key = lastKey = matcher.group(1);
                String value = matcher.group(2);
                // When "-f" is specified
                if (value.isEmpty()) {
                    // If right side was not provided, assume "true"
                    value = TRUE;
                }
                values.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } else if (lastKey != null) {
                //When "-f foo" is specified
                List<String> list = values.computeIfAbsent(lastKey, k -> new ArrayList<>());
                list.remove(TRUE);
                list.add(arg);
            }
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Entry<String, List<String>> entry : values.entrySet()) {
            map.put(entry.getKey(), String.join(",", entry.getValue()));
        }
        return map;
    }
}
