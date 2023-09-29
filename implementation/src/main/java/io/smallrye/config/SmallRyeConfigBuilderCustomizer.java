package io.smallrye.config;

/**
 * This {@code SmallRyeConfigBuilderCustomizer} allows to customize a {@link SmallRyeConfigBuilder}, used to create
 * a {@link SmallRyeConfig} instance.
 * <p>
 * Instances of this interface will be discovered via the {@link java.util.ServiceLoader} mechanism and can be
 * registered by providing a {@code META-INF/services/io.smallrye.config.SmallRyeConfigBuilderCustomizer} which
 * contains the fully qualified class name of the custom {@link SmallRyeConfigBuilderCustomizer} implementation.
 */
public interface SmallRyeConfigBuilderCustomizer {
    /**
     * Customize the current {@link SmallRyeConfigBuilder}.
     *
     * @param builder the current {@link SmallRyeConfigBuilder}.
     */
    void configBuilder(SmallRyeConfigBuilder builder);

    /**
     * Returns the customizer priority. Customizers are sorted by ascending priority and executed in that order, meaning
     * that higher numeric priorities will be executing last, possible overriding values set by previous customizers.
     *
     * @return the priority value.
     */
    default int priority() {
        return 0;
    }
}
