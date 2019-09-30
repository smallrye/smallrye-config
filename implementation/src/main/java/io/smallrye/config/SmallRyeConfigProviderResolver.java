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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class SmallRyeConfigProviderResolver extends ConfigProviderResolver {

    /**
     * @deprecated The instance stored in this field will generally not match the one instantiated by
     *             {@link ConfigProviderResolver}; thus this instance should not normally be used and may be removed
     *             from a future release. Instead use {@link ConfigProviderResolver#instance()} to get the instance.
     */
    @Deprecated
    public static final SmallRyeConfigProviderResolver INSTANCE = new SmallRyeConfigProviderResolver();

    private final Map<ClassLoader, Config> configsForClassLoader = new ConcurrentHashMap<>();

    private static final ClassLoader SYSTEM_CL;

    static {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            SYSTEM_CL = AccessController.doPrivileged((PrivilegedAction<ClassLoader>) SmallRyeConfigProviderResolver::calculateSystemClassLoader);
        } else {
            SYSTEM_CL = calculateSystemClassLoader();
        }
    }

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
        return getConfig(getContextClassLoader());
    }

    @Override
    public Config getConfig(ClassLoader classLoader) {
        classLoader = getRealClassLoader(classLoader);
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
        classLoader = getRealClassLoader(classLoader);
        synchronized (this) {
            if (configsForClassLoader.putIfAbsent(classLoader, config) != null) {
                throw new IllegalStateException("Configuration already registered for the given class loader");
            }
        }
    }

    @Override
    public void releaseConfig(Config config) {
        synchronized (this) {
            configsForClassLoader.values().remove(config);
        }
    }

    static ClassLoader getRealClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = SYSTEM_CL;
        }
        return classLoader;
    }
}
