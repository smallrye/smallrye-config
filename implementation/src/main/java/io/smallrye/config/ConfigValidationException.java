package io.smallrye.config;

public class ConfigValidationException extends RuntimeException {
    public ConfigValidationException() {
    }

    public ConfigValidationException(final String message) {
        super(message);
    }

    public ConfigValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConfigValidationException(final Throwable cause) {
        super(cause);
    }

    public ConfigValidationException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
