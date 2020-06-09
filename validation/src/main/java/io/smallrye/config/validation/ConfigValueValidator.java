package io.smallrye.config.validation;

public class ConfigValueValidator<T> {
    private final T value;
    private ConfigValueValidator<T> validator;

    public ConfigValueValidator(final T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}
