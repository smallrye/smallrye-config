package io.smallrye.config.validation;

/**
 * This will be the new ConfigValue. Added as separate class for the prototype and to try out the API.
 */
public interface ConfigValueNew {
    String getName();

    String getValue();

    <T> T getAs(Class<T> klass);

    ConfigValueNew min(long size);

    ConfigValueNew max(long size);

    ConfigValueNew pattern(String pattern);
}
