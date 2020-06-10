package io.smallrye.config;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigSourcePropertySubstitutionTest {
    @Test
    public void interceptor() {
        SmallRyeConfig config = (SmallRyeConfig) buildConfig("my.prop", "${prop.replace}", "prop.replace", "1234");

        final String value = config.getValue("my.prop", String.class);
        Assertions.assertEquals("1234", value);
    }

    private static Config buildConfig(String... keyValues) {
        return new SmallRyeConfigBuilder()
                .addDefaultSources()
                .withSources(KeyValuesConfigSource.config(keyValues))
                .withInterceptors(new PropertySubstitutionInterceptor())
                .build();
    }

    private static class PropertySubstitutionInterceptor implements ConfigSourceInterceptor {
        @Override
        public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
            final ConfigValue configValue = context.proceed(name);
            if (configValue != null) {
                final String replaced = StringPropertyReplacer.replaceProperties(configValue.getValue(), key -> {
                    final ConfigValue replace = context.proceed(key);
                    return replace != null ? replace.getValue() : null;
                });

                return configValue.withValue(replaced);
            }

            return null;
        }
    }

    private interface PropertyResolver {
        String getValue(String key);
    }

    private static final class StringPropertyReplacer {
        private static final int NORMAL = 0;
        private static final int SEEN_DOLLAR = 1;
        private static final int IN_BRACKET = 2;

        private static String replaceProperties(final String string, PropertyResolver propertyResolver) {
            final char[] chars = string.toCharArray();
            StringBuffer buffer = new StringBuffer();
            boolean properties = false;
            int state = NORMAL;
            int start = 0;
            for (int i = 0; i < chars.length; ++i) {
                char c = chars[i];

                // Dollar sign outside brackets
                if (c == '$' && state != IN_BRACKET) {
                    state = SEEN_DOLLAR;
                }

                // Open bracket immediately after dollar
                else if (c == '{' && state == SEEN_DOLLAR) {
                    buffer.append(string.substring(start, i - 1));
                    state = IN_BRACKET;
                    start = i - 1;
                }

                // No open bracket after dollar
                else if (state == SEEN_DOLLAR) {
                    state = NORMAL;
                }

                // Closed bracket after open bracket
                else if (c == '}' && state == IN_BRACKET) {
                    // No content
                    if (start + 2 == i) {
                        buffer.append("${}"); // REVIEW: Correct?
                    } else // Collect the system property
                    {
                        String value;

                        String key = string.substring(start + 2, i);

                        value = propertyResolver.getValue(key);

                        if (value != null) {
                            properties = true;
                            buffer.append(value);
                        } else {
                            buffer.append("${");
                            buffer.append(key);
                            buffer.append('}');
                        }
                    }
                    start = i + 1;
                    state = NORMAL;
                }
            }

            // No properties
            if (properties == false) {
                return string;
            }

            // Collect the trailing characters
            if (start != chars.length) {
                buffer.append(string.substring(start, chars.length));
            }

            // Done
            return buffer.toString();
        }
    }
}
