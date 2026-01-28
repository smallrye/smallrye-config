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
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.smallrye.config.SmallRyeConfigFactory.Default;
import io.smallrye.config._private.ConfigMessages;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class SmallRyeConfigProviderResolver extends ConfigProviderResolver {
    private final Map<ClassLoader, org.eclipse.microprofile.config.Config> configsForClassLoader = new ConcurrentHashMap<>();

    static final ClassLoader SYSTEM_CL;

    static {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            SYSTEM_CL = AccessController
                    .doPrivileged((PrivilegedAction<ClassLoader>) SmallRyeConfigProviderResolver::calculateSystemClassLoader);
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
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Map<ClassLoader, org.eclipse.microprofile.config.Config> configsForClassLoader = this.configsForClassLoader;
        org.eclipse.microprofile.config.Config config = configsForClassLoader.get(realClassLoader);
        if (config == null) {
            synchronized (configsForClassLoader) {
                config = configsForClassLoader.get(realClassLoader);
                if (config == null) {
                    config = getFactoryFor(realClassLoader, false).getConfigFor(this, classLoader);
                    // don't cache null, as that would leak class loaders
                    if (config == null) {
                        throw ConfigMessages.msg.noConfigForClassloader();
                    }
                    configsForClassLoader.put(realClassLoader, config);
                }
            }
        }
        return (Config) config;
    }

    /**
     * Get a {@link SmallRyeConfig} instance for the current {@link Thread#getContextClassLoader()}.
     *
     * @return the {@link SmallRyeConfig} instance for the thread context class loader
     * @throws IllegalArgumentException if the config is not registered for the thread context class loader
     */
    public SmallRyeConfig get() {
        return get(getContextClassLoader());
    }

    /**
     * Get a {@link SmallRyeConfig} instance for the specified {@link ClassLoader}.
     *
     * @return the {@link SmallRyeConfig} instance for the specified class loader
     * @throws IllegalArgumentException if the config is not registered for the thread context class loader
     */
    public SmallRyeConfig get(ClassLoader classLoader) {
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        org.eclipse.microprofile.config.Config config = configsForClassLoader.get(realClassLoader);
        if (config == null) {
            throw ConfigMessages.msg.noConfigForClassloader();
        }
        return ((SmallRyeConfig) config);
    }

    SmallRyeConfigFactory getFactoryFor(final ClassLoader classLoader, final boolean privileged) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null && !privileged) {
            // run privileged so that the only things on the access control stack are us and the provider
            return AccessController.doPrivileged(new PrivilegedAction<SmallRyeConfigFactory>() {
                public SmallRyeConfigFactory run() {
                    return getFactoryFor(classLoader, true);
                }
            });
        }
        ServiceLoader<SmallRyeConfigFactory> serviceLoader = ServiceLoader.load(SmallRyeConfigFactory.class, classLoader);
        Iterator<SmallRyeConfigFactory> iterator = serviceLoader.iterator();
        return iterator.hasNext() ? iterator.next() : Default.INSTANCE;
    }

    @Override
    public SmallRyeConfigBuilder getBuilder() {
        return new SmallRyeConfigBuilder().addDefaultInterceptors();
    }

    @Override
    public void registerConfig(org.eclipse.microprofile.config.Config config, ClassLoader classLoader) {
        if (config == null) {
            throw ConfigMessages.msg.configIsNull();
        }
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Map<ClassLoader, org.eclipse.microprofile.config.Config> configsForClassLoader = this.configsForClassLoader;
        synchronized (configsForClassLoader) {
            org.eclipse.microprofile.config.Config existing = configsForClassLoader.putIfAbsent(realClassLoader, config);
            if (existing != null) {
                throw ConfigMessages.msg.configAlreadyRegistered();
            }
        }
    }

    @Override
    public void releaseConfig(org.eclipse.microprofile.config.Config config) {
        // todo: see https://github.com/eclipse/microprofile-config/issues/136#issuecomment-535962313
        // todo: see https://github.com/eclipse/microprofile-config/issues/471
        Map<ClassLoader, org.eclipse.microprofile.config.Config> configsForClassLoader = this.configsForClassLoader;
        synchronized (configsForClassLoader) {
            // let's avoid a lambda here as it is called on startup
            Iterator<Map.Entry<ClassLoader, org.eclipse.microprofile.config.Config>> it = configsForClassLoader.entrySet()
                    .iterator();
            while (it.hasNext()) {
                if (it.next().getValue() == config) {
                    it.remove();
                }
            }
        }
    }

    public void releaseConfig(ClassLoader classLoader) {
        ClassLoader realClassLoader = getRealClassLoader(classLoader);
        Map<ClassLoader, org.eclipse.microprofile.config.Config> configsForClassLoader = this.configsForClassLoader;
        synchronized (configsForClassLoader) {
            configsForClassLoader.remove(realClassLoader);
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
