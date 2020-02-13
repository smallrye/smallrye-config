/*
 * Copyright 2020 Red Hat, Inc.
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

package io.smallrye.config.source.kubernetes;

import io.smallrye.config.common.AbstractConfigSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

public class KubernetesConfigSource extends AbstractConfigSource {

    //Property the directory of the Kubernetes volumes will be read from
    private static final String KUBERNETES_VOLUME_KEY = "io.smallrye.configsource.kubernetes.volume";
    private static final Logger LOG = Logger.getLogger("io.smallrye.config");

    protected static final int KUBERNETES_ORDINAL = ConfigSource.DEFAULT_ORDINAL + 10;

    protected final Path volumePath;
    protected volatile Map<String, String> properties = new ConcurrentHashMap<>();

    public KubernetesConfigSource(String name, int ordinal) {
        super(name, ordinal);

        final Config cfg = ConfigProvider.getConfig();
        final Optional<String> baseDirectory = cfg.getOptionalValue(KUBERNETES_VOLUME_KEY, String.class);

        if (baseDirectory.isPresent()) {
            this.volumePath = Paths.get(baseDirectory.get());
        } else {
            throw new IllegalArgumentException(
                "Please set property for \"" + KUBERNETES_VOLUME_KEY + "\"");
        }

        init();
    }

    public KubernetesConfigSource(Path volumePath, String name, int ordinal) {
        super(name, ordinal);
        this.volumePath = volumePath;

        init();
    }

    public KubernetesConfigSource(Path volumePath) {
        this(volumePath, volumePath.getFileName().toString(), KUBERNETES_ORDINAL);
    }

    void init() {
        scan();
    }

    protected List<ConfigSourceParser> initConfigSourceParserChain() {
        return Arrays.asList(new YamlConfigSourceParser(), new PropertiesConfigSourceParser());
    }

    protected List<ConfigSourceParser> postConfigSource() {
        return Arrays.asList(new FileSystemConfigSourceParser());
    }

    protected void scan() {
            if (Files.isDirectory(this.volumePath)) {
                final List<ConfigSourceParser> configSourceParsers = initConfigSourceParserChain();
                try (Stream<Path> stream = Files.walk(this.volumePath, 1)) {
                     stream
                        .filter(Files::isRegularFile)
                        .forEach(p -> {
                            populateProperties(p, configSourceParsers);
                        });
                } catch (Exception e) {
                    LOG.warnf("Unable to read content from file %s. Exception: %s", this.volumePath.toAbsolutePath(),
                        e.getLocalizedMessage());
                }
                executePostConfigSource();
            }
    }

    void executePostConfigSource() {
        postConfigSource().stream().forEach(c -> {
            final ConfigSource configSource = c.parser(this.volumePath);
            this.properties.putAll(configSource.getProperties());
        });
    }

    boolean populateProperties(final Path path, final List<ConfigSourceParser> configSourceParsers) {
        for (ConfigSourceParser configSourceParser : configSourceParsers) {
            if (configSourceParser.isSupported(path)) {
                final ConfigSource configSource = configSourceParser.parser(path);
                this.properties.putAll(configSource.getProperties());
                return true;
            }
        }

        return false;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    public Future<?> startMonitoringChanges() {
        return this.startMonitoringChanges(Executors.newFixedThreadPool(1));
    }

    public Future<?> startMonitoringChanges(final ExecutorService executorService) {
        return executorService.submit(new VolumeChangeWatcher(this));
    }

}
