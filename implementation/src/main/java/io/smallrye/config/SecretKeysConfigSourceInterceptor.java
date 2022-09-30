package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.DOUBLE_COLON;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import jakarta.annotation.Priority;

import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;

@Priority(Priorities.LIBRARY + 100)
public class SecretKeysConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 7291982039729980590L;

    private final Set<String> secrets;
    private final Map<String, SecretKeysHandler> handlers;

    public SecretKeysConfigSourceInterceptor(final Set<String> secrets) {
        this(secrets, Collections.emptyMap());
    }

    public SecretKeysConfigSourceInterceptor(final Set<String> secrets, final Map<String, SecretKeysHandler> handlers) {
        this.secrets = secrets;
        this.handlers = handlers;
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        if (SecretKeys.isLocked() && isSecret(name)) {
            throw ConfigMessages.msg.notAllowed(name);
        }
        ConfigValue configValue = context.proceed(name);

        if (configValue == null || handlers.isEmpty()) {
            return configValue;
        }

        // This executes before the Expression resolution.
        // If we find a handler we execute it, if not we proceed
        Expression expression = Expression.compile(configValue.getValue(), LENIENT_SYNTAX, NO_TRIM, NO_SMART_BRACES,
                DOUBLE_COLON);
        String secret = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(final ResolveContext<RuntimeException> context, final StringBuilder stringBuilder) {
                String[] split = context.getKey().split("::");
                if (split.length == 2) {
                    String key = split[0];
                    String secret = split[1];

                    SecretKeysHandler handler = handlers.get(key);
                    if (handler != null) {
                        stringBuilder.append(handler.handleSecret(secret));
                    }
                }

                // TODO - handle errors
            }
        });

        return secret != null && !secret.isEmpty() ? configValue.withValue(secret) : configValue;
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        if (SecretKeys.isLocked()) {
            Set<String> names = new HashSet<>();
            Iterator<String> namesIterator = context.iterateNames();
            while (namesIterator.hasNext()) {
                String name = namesIterator.next();
                if (!secrets.contains(name)) {
                    names.add(name);
                }
            }
            return names.iterator();
        }
        return context.iterateNames();
    }

    @Override
    public Iterator<ConfigValue> iterateValues(final ConfigSourceInterceptorContext context) {
        if (SecretKeys.isLocked()) {
            Set<ConfigValue> values = new HashSet<>();
            Iterator<ConfigValue> valuesIterator = context.iterateValues();
            while (valuesIterator.hasNext()) {
                ConfigValue value = valuesIterator.next();
                if (!secrets.contains(value.getName())) {
                    values.add(value);
                }
            }
            return values.iterator();
        }
        return context.iterateValues();
    }

    private boolean isSecret(final String name) {
        return secrets.contains(name);
    }
}
