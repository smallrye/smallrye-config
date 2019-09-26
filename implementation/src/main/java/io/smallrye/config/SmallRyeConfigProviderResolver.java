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

import static io.smallrye.config.SecuritySupport.getContextClassLoader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class SmallRyeConfigProviderResolver extends ConfigProviderResolver {

    public static final SmallRyeConfigProviderResolver INSTANCE = new SmallRyeConfigProviderResolver();

    private final Map<ClassLoader, Config> configsForClassLoader = new HashMap<>();

    @Override
    public Config getConfig() {
        return getConfig(getContextClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader classLoader) {
        Config config = configsForClassLoader.get(classLoader);
        if (config == null) {
            synchronized (this) {
                config = configsForClassLoader.get(classLoader);
                if (config == null) {
                    config = getBuilder().forClassLoader(classLoader)
                            .addDefaultSources()
                            .addDiscoveredSources()
                            .addDiscoveredConverters()
                            .build();
                    registerConfig(config, classLoader);
                }
            }
        }
        return config;
    }

    @Override
    public SmallRyeConfigBuilder getBuilder() {
        return new SmallRyeConfigBuilder();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        synchronized (this) {
            configsForClassLoader.put(classLoader != null ? classLoader : getContextClassLoader(), config);
        }
    }

    @Override
    public void releaseConfig(Config config) {
        synchronized (this) {
            Iterator<Map.Entry<ClassLoader, Config>> iterator = configsForClassLoader.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ClassLoader, Config> entry = iterator.next();
                if (entry.getValue() == config) {
                    iterator.remove();
                    return;
                }
            }
        }
    }
}
