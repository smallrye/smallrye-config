package io.smallrye.config;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * This interceptor adds additional entries to {@link org.eclipse.microprofile.config.Config#getPropertyNames}.
 *
 * The additional entries are required when a consumer relies on the list of properties to find additional
 * configurations. The list of properties is not normalized due to environment variables, which follow specific naming
 * rules. The MicroProfile Config specification defines a set of conversion rules to look up and find values from
 * environment variables even when using their dotted version, but this does not apply to the properties list.
 *
 * Because an environment variable name may only be represented by a subset of characters, it is not possible to
 * represent exactly a dotted version name from an environment variable name. Additional dotted properties mapped from
 * environment variables are only added if a relationship cannot be found between all properties using the conversions
 * look up rules of the MicroProfile Config specification. Example:
 *
 * If <code>foo.bar</code> is present and <code>FOO_BAR</code> is also present, no additional property is required.
 * If <code>foo-bar</code> is present and <code>FOO_BAR</code> is also present, no additional property is required.
 * If <code>FOO_BAR</code> is present an additional property <code>foo.bar</code> is added.
 */
class PropertyNamesConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 5263983885197566053L;

    private final Set<String> dottedProperties = new HashSet<>();

    public PropertyNamesConfigSourceInterceptor(final Set<String> properties, final List<ConfigSource> sources) {
        final Set<String> envProperties = new HashSet<>();
        for (ConfigSource source : sources) {
            if (source instanceof EnvConfigSource) {
                envProperties.addAll(source.getPropertyNames());
            }
        }
        properties.removeAll(envProperties);

        final Set<String> overrides = new HashSet<>();
        for (String property : properties) {
            for (String envProperty : envProperties) {
                if (envProperty.equalsIgnoreCase(replaceNonAlphanumericByUnderscores(property))) {
                    overrides.add(envProperty);
                    break;
                }
            }
        }

        envProperties.removeAll(overrides);
        for (String envProperty : envProperties) {
            dottedProperties.add(toLowerCaseAndDotted(envProperty));
        }
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
        names.addAll(dottedProperties);
        return names.iterator();
    }

    private static String toLowerCaseAndDotted(final String name) {
        int length = name.length();
        boolean quotesOpen = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if ('_' == c) {
                int j = i + 1;
                if (j < length) {
                    if ('_' == name.charAt(j) && !quotesOpen) {
                        sb.append(".");
                        sb.append("\"");
                        i = j;
                        quotesOpen = true;
                    } else if ('_' == name.charAt(j) && quotesOpen) {
                        sb.append("\"");
                        sb.append(".");
                        i = j;
                        quotesOpen = false;
                    } else {
                        sb.append(".");
                    }
                } else {
                    sb.append(".");
                }
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
