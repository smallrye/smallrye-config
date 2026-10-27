package io.smallrye.config.spring;

import java.lang.reflect.Field;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappingHandler;

/**
 * A {@link ConfigMappingHandler} for Spring Boot
 * {@link ConfigurationProperties @ConfigurationProperties}-annotated classes.
 * <p>
 * Extracts the configuration prefix from {@link ConfigurationProperties#prefix()} (or {@link ConfigurationProperties#value()})
 * and field-level name overrides from {@link Name @Name} annotations.
 * <p>
 * Spring Boot does not declare a naming strategy, so the class may additionally be annotated with
 * {@link ConfigMapping @ConfigMapping} to set {@link ConfigMapping#namingStrategy()}. A nested class declared inside
 * an annotated class inherits the strategy of its closest annotated enclosing class. Without an annotation, the
 * strategy is {@link NamingStrategy#KEBAB_CASE}.
 * <p>
 * Configuration names under the class prefix that do not map to any field are ignored, unless
 * {@link ConfigurationProperties#ignoreUnknownFields()} is {@code false}, in which case they are reported as
 * validation errors.
 */
@Experimental("Spring @ConfigurationProperties is experimental and targeting Quarkus")
public final class ConfigurationPropertiesMappingHandler implements ConfigMappingHandler {
    @Override
    public boolean handles(final Class<?> type) {
        if (!type.isEnum() && !type.isInterface() && !type.isArray() && !type.isPrimitive()) {
            return type.isAnnotationPresent(ConfigurationProperties.class);
        }
        return false;
    }

    @Override
    public FieldMember processField(final Field field) {
        Name name = field.getAnnotation(Name.class);
        if (name != null) {
            return new FieldMember(name.value(), null, null);
        }
        return FieldMember.EMPTY;
    }

    @Override
    public String getPrefix(final Class<?> type) {
        ConfigurationProperties configurationProperties = type.getAnnotation(ConfigurationProperties.class);
        if (configurationProperties != null) {
            String prefix = configurationProperties.prefix();
            if (prefix.isEmpty()) {
                prefix = configurationProperties.value();
            }
            return prefix;
        }
        return "";
    }

    @Override
    public NamingStrategy getNamingStrategy(final Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
            ConfigMapping configMapping = current.getAnnotation(ConfigMapping.class);
            if (configMapping != null) {
                return configMapping.namingStrategy();
            }
        }
        return NamingStrategy.KEBAB_CASE;
    }

    @Override
    public boolean ignoreUnmappedProperties() {
        return true;
    }

    @Override
    public boolean ignoreUnmappedProperties(final Class<?> type) {
        ConfigurationProperties configurationProperties = type.getAnnotation(ConfigurationProperties.class);
        return configurationProperties == null || configurationProperties.ignoreUnknownFields();
    }
}
