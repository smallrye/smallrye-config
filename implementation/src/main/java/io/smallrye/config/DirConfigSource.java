/*
 * Copyright 2017 Red Hat, Inc.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

/**
 * Read configuration from a file directory.
 *
 * Each file in the directory corresponds to a property where the file name is the property key
 * and the file textual content is the property value.
 *
 * For example, if a directory structure looks like:
 *
 * <pre>
 * <code>
 * foo/
 * ├── num.max
 * └── num.size
 * </code>
 * </pre>
 *
 * <code>new DirConfigSource("foo")</code> will provide 2 properties:
 * <ul>
 * <li><code>num.max</code></li>
 * <li><code>num.size</code></li>
 * </ul>
 *
 * Nested directories are not supported.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class DirConfigSource implements ConfigSource {

    private static final Logger LOG = Logger.getLogger("io.smallrye.config");

    private static final String CONFIG_ORDINAL_KEY = "config_ordinal";
    private static final String CONFIG_ORDINAL_DEFAULT_VALUE = "100";

    private final File dir;
    private final int ordinal;
    private final Map<String, String> props;

    DirConfigSource(File dir) {
        this(dir, DEFAULT_ORDINAL);
    }

    public DirConfigSource(File dir, int ordinal) {
        this.dir = dir;
        this.props = scan();
        if (props.containsKey(CONFIG_ORDINAL_KEY)) {
            this.ordinal = Integer.valueOf(props.getOrDefault(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_DEFAULT_VALUE));
        } else {
            this.ordinal = ordinal;
        }
    }

    private Map<String, String> scan() {
        Map<String, String> props = new HashMap<>();
        if (dir == null || !dir.isDirectory()) {
            return props;
        }
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            try {
                String key = file.getName();
                String value = readContent(file);
                props.put(key, value);
            } catch (Throwable t) {
                LOG.warnf("Unable to read content from file %s", file.getAbsolutePath());
            }
        }
        return props;
    }

    private String readContent(File file) throws IOException {
        String content = Files.lines(file.toPath())
                .collect(Collectors.joining());
        return content;
    }

    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    @Override
    public String getValue(String key) {
        return props.get(key);
    }

    @Override
    public String getName() {
        return "DirConfigSource[dir=" + dir.getAbsolutePath() + "]";
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }
}
