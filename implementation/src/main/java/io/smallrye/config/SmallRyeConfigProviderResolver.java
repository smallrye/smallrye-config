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

import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.smallrye.config._private.ConfigMessages;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class SmallRyeConfigProviderResolver extends ConfigProviderResolver {
    private final Map<ClassLoader, Config> configsForClassLoader = new ConcurrentHashMap<>();

    private static final ClassLoader SYSTEM_CL = calculateSystemClassLoader();

    public SmallRyeConfigProviderResolver() {
    }

    private static ClassLoader calculateSystemClassLoader() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        if (cl == null) {
            // non-null ref that delegates to the system
            cl = new ClassLoader(null) {
            };
        }
        return cl;
    }

    @Override
    public Config getConfig() {
        return getConfig(Thread.currentThread().getContextClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader classLoader) {
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Map<ClassLoader, Config> configsForClassLoader = this.configsForClassLoader;
        Config config = configsForClassLoader.get(realClassLoader);
        if (config == null) {
            synchronized (configsForClassLoader) {
                config = configsForClassLoader.get(realClassLoader);
                if (config == null) {
                    config = getFactoryFor(realClassLoader).getConfigFor(this, classLoader);
                    // don't cache null, as that would leak class loaders
                    if (config == null) {
                        throw ConfigMessages.msg.noConfigForClassloader();
                    }
                    configsForClassLoader.put(realClassLoader, config);
                }
            }
        }
        return config;
    }

    SmallRyeConfigFactory getFactoryFor(final ClassLoader classLoader) {
        ServiceLoader<SmallRyeConfigFactory> serviceLoader = ServiceLoader.load(SmallRyeConfigFactory.class, classLoader);
        Iterator<SmallRyeConfigFactory> iterator = serviceLoader.iterator();
        return iterator.hasNext() ? iterator.next() : SmallRyeConfigFactory.Default.INSTANCE;
    }

    @Override
    public SmallRyeConfigBuilder getBuilder() {
        return new SmallRyeConfigBuilder().addDefaultInterceptors();
    }

    @Override
    public void registerConfig(Config config, ClassLoader classLoader) {
        if (config == null) {
            throw ConfigMessages.msg.configIsNull();
        }
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Map<ClassLoader, Config> configsForClassLoader = this.configsForClassLoader;
        synchronized (configsForClassLoader) {
            final Config existing = configsForClassLoader.putIfAbsent(realClassLoader, config);
            if (existing != null) {
                throw ConfigMessages.msg.configAlreadyRegistered();
            }
        }
    }

    @Override
    public void releaseConfig(Config config) {
        // todo: see https://github.com/eclipse/microprofile-config/issues/136#issuecomment-535962313
        // todo: see https://github.com/eclipse/microprofile-config/issues/471
        Map<ClassLoader, Config> configsForClassLoader = this.configsForClassLoader;
        synchronized (configsForClassLoader) {
            configsForClassLoader.values().removeIf(v -> v == config);
        }
    }

    public void releaseConfig(ClassLoader classLoader) {
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Map<ClassLoader, Config> configsForClassLoader = this.configsForClassLoader;
        synchronized (configsForClassLoader) {
            configsForClassLoader.remove(realClassLoader);
        }
    }

    static ClassLoader getRealClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = SYSTEM_CL;
        }
        return classLoader;
    }
}
