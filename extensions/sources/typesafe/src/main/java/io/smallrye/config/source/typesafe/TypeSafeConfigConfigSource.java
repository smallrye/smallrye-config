/*
 * Copyright 2019 Red Hat, Inc.
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
package io.smallrye.config.source.typesafe;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import io.smallrye.config.source.EnabledConfigSource;
import lombok.extern.java.Log;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * MicroProfile Config {@link ConfigSource} which is backed by the TypeSafe Config configuration
 * capabilities. By default, unless a explicit config is provided this will leverage the default
 * TypeSafe config instance.
 */
@Log
public class TypeSafeConfigConfigSource extends EnabledConfigSource {

    private static final String NAME = "TypeSafeConfigConfigSource";
    private Config config;

    /**
     * Create a Config Source that is backed by the default TypeSafe config via {@link ConfigFactory#load()}.
     */
    public TypeSafeConfigConfigSource() {
        this(ConfigFactory.load());
    }

    /**
     * Create a Config Source that is backed by a provided TypeSafe config.
     * @param config The configuration to use as the backing configuration. Any key requested will be traversed from
     *               the root of this config.
     */
    public TypeSafeConfigConfigSource(Config config) {
        super();
        this.config = config;
        this.initOrdinal(310);
    }

    @Override
    public Set<String> getPropertyNames() {
        // Not sure how often this is called if we need to do any sort of caching or not.
        return config.entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<String, String> getPropertiesIfEnabled() {
        log.fine("getProperties");
        HashMap<String, String> props = new HashMap<>();
        config.entrySet()
                .forEach(entry ->
                        props.put(entry.getKey(), config.getString(entry.getKey())));
        return props;
    }

    @Override
    public String getValue(String key) {
        try {
            log.log(Level.FINE, "load {0} from typesafe config", key);
            return config.getString(key);
        } catch (ConfigException ex) {
            log.log(Level.WARNING, "Config key '{0}' could not be retrieved due to ConfigException with "
                    + "message '{1}'. To see stack trace enable trace logging", new String[]{key, ex.getMessage()});
            log.log(Level.FINER, "ConfigException stack trace", ex);
            return null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
