package io.smallrye.config;

/**
 * Receives the configuration names that were not used by any {@link ConfigMapping}, when the {@link SmallRyeConfig}
 * instance is created.
 * <p>
 * A reporter is meant for cases where a {@link SmallRyeConfig} instance does not know every mapping, so it can't decide
 * by itself if a name is unknown. It sees every unused name, including names outside the mapping prefixes, or ignored
 * with {@link SmallRyeConfigBuilder#withMappingIgnore(String)}. The names it accepts are handled only by the reporter.
 * All the others follow the regular validation.
 * <p>
 * The reporter runs even if the validation is disabled with {@link SmallRyeConfigBuilder#withValidateUnknown(boolean)},
 * and only if the {@link SmallRyeConfig} instance has at least one mapping.
 *
 * @see SmallRyeConfigBuilder#withUnknownReporter(ConfigUnknownReporter)
 */
public interface ConfigUnknownReporter {
    /**
     * A reporter that does not handle any configuration name, so every name follows the regular validation.
     */
    ConfigUnknownReporter EMPTY = new ConfigUnknownReporter() {
        @Override
        public boolean accepts(final String name) {
            return false;
        }

        @Override
        public void report(final String name, final ConfigValue configValue) {
        }
    };

    /**
     * A reporter that does not handle any configuration name, and sets the regular validation to log a warning for
     * each unknown name, instead of failing the {@link SmallRyeConfig} creation.
     */
    ConfigUnknownReporter WARN = new ConfigUnknownReporter() {
        @Override
        public Mode mode() {
            return Mode.WARN;
        }

        @Override
        public boolean accepts(final String name) {
            return false;
        }

        @Override
        public void report(final String name, final ConfigValue configValue) {
        }
    };

    /**
     * How the regular validation reports the unknown configuration names that this reporter does not accept. The mode
     * does not apply if the validation is disabled with {@link SmallRyeConfigBuilder#withValidateUnknown(boolean)}.
     *
     * @return the {@link Mode}, {@link Mode#ERROR} by default
     */
    default Mode mode() {
        return Mode.ERROR;
    }

    /**
     * Whether this reporter handles the configuration name. This method is called for every unused name, so it should
     * be cheap, and it is called before looking up the name value.
     *
     * @param name the configuration name
     * @return <code>true</code> if this reporter handles the name, or <code>false</code> to apply the regular validation
     */
    boolean accepts(String name);

    /**
     * Reports an unused configuration name. A name provided by an {@link EnvConfigSource} is never reported, because
     * an environment variable name cannot be matched reliably to a configuration name.
     *
     * @param name the configuration name
     * @param configValue the {@link ConfigValue} found for the name. The {@link ConfigValue#getName()} is different from
     *        <code>name</code> if the name was relocated or has a fallback.
     */
    void report(String name, ConfigValue configValue);

    /**
     * How a {@link SmallRyeConfig} reports a configuration name in a {@link ConfigMapping} prefix that does not map to
     * any member of the mapping.
     *
     * @see ConfigUnknownReporter#mode()
     */
    enum Mode {
        /**
         * Fails the {@link SmallRyeConfig} creation with a {@link ConfigValidationException}.
         */
        ERROR,
        /**
         * Logs a warning, and the {@link SmallRyeConfig} creation continues.
         */
        WARN
    }
}
