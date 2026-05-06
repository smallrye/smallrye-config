package io.smallrye.config.microprofile;

import java.lang.reflect.Field;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.smallrye.config.ConfigMappingHandler;
import io.smallrye.config._private.ConfigMessages;

/**
 * A {@link ConfigMappingHandler} for MicroProfile Config {@link ConfigProperties @ConfigProperties} annotated classes.
 * <p>
 * Extracts the configuration prefix from {@link ConfigProperties#prefix()} and field-level metadata from
 * {@link ConfigProperty @ConfigProperty} annotations (name and default value).
 */
public final class ConfigPropertiesConfigMappingHandler implements ConfigMappingHandler {
    @Override
    public boolean handles(Class<?> classType) {
        if (!classType.isEnum() && !classType.isInterface() && !classType.isArray() && !classType.isPrimitive()) {
            return classType.isAnnotationPresent(ConfigProperties.class);
        }
        if (classType.isAnnotationPresent(ConfigProperties.class)) {
            throw ConfigMessages.msg.propertiesAnnotationNotSupportedInInterface(classType);
        }
        return false;
    }

    @Override
    public FieldMember processField(final Field field) {
        ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            String name = !configProperty.name().isEmpty() ? configProperty.name() : null;
            String defaultValue = !configProperty.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE)
                    ? configProperty.defaultValue()
                    : null;
            return new FieldMember(name, defaultValue, null);
        }
        return FieldMember.EMPTY;
    }

    @Override
    public boolean ignoreUnmappedProperties() {
        return true;
    }

    @Override
    public String getPrefix(Class<?> classType) {
        ConfigProperties configProperties = classType.getAnnotation(ConfigProperties.class);
        String prefix = configProperties != null ? configProperties.prefix() : "";
        if (prefix.equals(ConfigProperties.UNCONFIGURED_PREFIX)) {
            prefix = "";
        }
        return prefix;
    }
}
