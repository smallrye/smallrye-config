package io.smallrye.config.validation;

public interface ConfigValueValidator extends ConfigValueNew {
    ConfigValueValidator max(final long value);

    ConfigValueValidator min(final long value);
}
