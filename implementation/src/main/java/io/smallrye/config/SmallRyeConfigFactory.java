package io.smallrye.config;

/**
 * A factory which allows a user-provided strategy for locating, creating, and configuring the configuration instance
 * which corresponds to a given class loader. The factory will be searched for on the class loader that
 * was submitted to {@link SmallRyeConfigProviderResolver#getConfig(ClassLoader)}.
 * <p>
 * Since the factory is given access to a class loader, subclasses are checked for the {@code getClassLoader}
 * {@link RuntimePermission} on instantiation if a security manager is present.
 * <p>
 * The default implementation will create and configure a configuration with the set of discovered
 * configuration sources, the set of discovered configuration converters and the set of discoverd interceptors
 * from the given class loader.
 */
public abstract class SmallRyeConfigFactory {
    /**
     * Construct a new instance. Callers will be checked for the {@code getClassLoader}
     * {@link RuntimePermission}.
     */
    protected SmallRyeConfigFactory() {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        }
    }

    /**
     * Get the configuration object for the given class loader. If the method returns {@code null},
     * {@link SmallRyeConfigProviderResolver#getConfig(ClassLoader)} will throw an exception for the given
     * class loader indicating that no configuration is available. Any other exception thrown by this method
     * will be thrown directly to callers of the above method.
     * <p>
     * The provided class loader will be {@code null} if {@linkplain ClassLoader#getSystemClassLoader() the system class loader}
     * is {@code null}. In this case, the system class loader should be used to search for classes or resources.
     *
     * @param configProviderResolver the configuration provider resolver (not {@code null})
     * @param classLoader the class loader (possibly {@code null})
     * @return the configuration object, or {@code null} if there is no configuration available for the given class loader
     */
    public abstract SmallRyeConfig getConfigFor(SmallRyeConfigProviderResolver configProviderResolver, ClassLoader classLoader);

    /**
     * The default configuration factory.
     */
    static final class Default extends SmallRyeConfigFactory {

        static final Default INSTANCE = new Default();

        Default() {
        }

        public SmallRyeConfig getConfigFor(SmallRyeConfigProviderResolver configProviderResolver, ClassLoader classLoader) {
            return configProviderResolver.getBuilder().forClassLoader(classLoader)
                    .addDefaultSources()
                    .addDefaultInterceptors()
                    .addDiscoveredSources()
                    .addDiscoveredConverters()
                    .addDiscoveredInterceptors()
                    .build();
        }
    }
}
