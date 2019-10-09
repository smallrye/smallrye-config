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

import static io.smallrye.config.utils.ConfigSourceUtil.CONFIG_ORDINAL_100;
import static io.smallrye.config.utils.ConfigSourceUtil.CONFIG_ORDINAL_KEY;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

/**
 * Read configuration from a file directory.
 * <p>
 * Each file in the directory corresponds to a property where the file name is the property key
 * and the file textual content is the property value.
 * <p>
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
 * <p>
 * Nested directories are not supported.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class DirConfigSource implements ConfigSource {

    private static final Logger LOG = Logger.getLogger("io.smallrye.config");

    private final File dir;
    private final int ordinal;
    private final Map<String, String> props;

    DirConfigSource(File dir) {
        this(dir, DEFAULT_ORDINAL);
    }

    /**
     * Construct a new instance
     *
     * @param dir the directory, containing configuration files
     * @param ordinal the ordinal value
     */
    public DirConfigSource(File dir, int ordinal) {
        this.dir = dir;
        this.props = scan(dir);
        if (props.containsKey(CONFIG_ORDINAL_KEY)) {
            this.ordinal = Integer.parseInt(props.getOrDefault(CONFIG_ORDINAL_KEY, CONFIG_ORDINAL_100));
        } else {
            this.ordinal = ordinal;
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(props);
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

    private Map<String, String> scan(File directory) {
        if (directory != null && directory.isDirectory()) {
            try (Stream<Path> stream = Files.walk(directory.toPath())) {

                return stream.filter(p -> p.toFile().isFile())
                        .collect(Collectors.toMap(it -> it.getFileName().toString(), this::readContent));
            } catch (Throwable t) {
                LOG.warnf("Unable to read content from file %s", directory.getAbsolutePath());
            }
        }
        return Collections.emptyMap();
    }

    private String readContent(Path file) {
        try (Stream<String> stream = Files.lines(file)) {
            return stream.collect(Collectors.joining());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
