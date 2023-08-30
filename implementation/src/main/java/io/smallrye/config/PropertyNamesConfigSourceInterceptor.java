package io.smallrye.config;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static io.smallrye.config.common.utils.StringUtil.toLowerCaseAndDotted;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * This interceptor adds additional entries to {@link org.eclipse.microprofile.config.Config#getPropertyNames}.
 */
class PropertyNamesConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 5263983885197566053L;

    private final Set<String> properties = new HashSet<>();

    public PropertyNamesConfigSourceInterceptor(final ConfigSourceInterceptorContext context,
            final List<ConfigSource> sources) {
        this.properties.addAll(generateDottedProperties(context, sources));
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return context.proceed(name);
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        final Set<String> names = new HashSet<>();
        final Iterator<String> namesIterator = context.iterateNames();
        while (namesIterator.hasNext()) {
            names.add(namesIterator.next());
        }
        names.addAll(properties);
        return names.iterator();
    }

    void addProperties(final Set<String> properties) {
        this.properties.addAll(properties);
    }

    /**
     * Generate dotted properties from Env properties.
     * <br>
     * These are required when a consumer relies on the list of properties to find additional
     * configurations. The list of properties is not normalized due to environment variables, which follow specific
     * naming rules. The MicroProfile Config specification defines a set of conversion rules to look up and find
     * values from environment variables even when using their dotted version, but this does not apply to the
     * properties list.
     * <br>
     * Because an environment variable name may only be represented by a subset of characters, it is not possible
     * to represent exactly a dotted version name from an environment variable name. Additional dotted properties
     * mapped from environment variables are only added if a relationship cannot be found between all properties
     * using the conversions look up rules of the MicroProfile Config specification. Example:
     * <br>
     * If <code>foo.bar</code> is present and <code>FOO_BAR</code> is also present, no property is required.
     * If <code>foo-bar</code> is present and <code>FOO_BAR</code> is also present, no property is required.
     * If <code>FOO_BAR</code> is present a property <code>foo.bar</code> is required.
     */
    private static Set<String> generateDottedProperties(final ConfigSourceInterceptorContext current,
            final List<ConfigSource> sources) {
        // Collect all known properties
        Set<String> properties = new HashSet<>();
        Iterator<String> iterateNames = current.iterateNames();
        while (iterateNames.hasNext()) {
            properties.add(iterateNames.next());
        }

        // Collect only properties from the EnvSources
        Set<String> envProperties = new HashSet<>();
        for (ConfigSource source : sources) {
            if (source instanceof EnvConfigSource) {
                envProperties.addAll(source.getPropertyNames());
            }
        }
        properties.removeAll(envProperties);

        // Collect properties that have the same semantic meaning
        Set<String> overrides = new HashSet<>();
        for (String property : properties) {
            String semanticProperty = replaceNonAlphanumericByUnderscores(property);
            for (String envProperty : envProperties) {
                if (envProperty.equalsIgnoreCase(semanticProperty)) {
                    overrides.add(envProperty);
                    break;
                }
            }
        }

        // Remove them - Remaining properties can only be found in the EnvSource - generate a dotted version
        envProperties.removeAll(overrides);
        Set<String> dottedProperties = new HashSet<>();
        for (String envProperty : envProperties) {
            dottedProperties.add(toLowerCaseAndDotted(envProperty));
        }
        return dottedProperties;
    }
}
