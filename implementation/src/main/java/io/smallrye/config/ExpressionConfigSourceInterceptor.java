package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.DOUBLE_COLON;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;
import static io.smallrye.config._private.ConfigMessages.msg;

import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import jakarta.annotation.Priority;

import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;
import io.smallrye.config.ConfigValidationException.Problem;
import io.smallrye.config._private.ConfigMessages;

@Priority(Priorities.LIBRARY + 300)
public class ExpressionConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Serial
    private static final long serialVersionUID = -539336551011916218L;

    private static final int MAX_DEPTH = 32;

    private final boolean enabled;
    private final Map<String, SecretKeysHandler> handlers = new HashMap<>();

    public ExpressionConfigSourceInterceptor() {
        this(true, Collections.emptyList());
    }

    public ExpressionConfigSourceInterceptor(final boolean enabled, final List<SecretKeysHandler> handlers) {
        this.enabled = enabled;
        for (SecretKeysHandler handler : handlers) {
            this.handlers.put(handler.getName(), handler);
        }
    }

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return getValue(context, name, 1);
    }

    private ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name, final int depth) {
        if (depth >= MAX_DEPTH) {
            throw msg.expressionExpansionTooDepth(name);
        }

        ConfigValue configValue = context.proceed(name);

        if (!Expressions.isEnabled() || !enabled) {
            return configValue;
        }

        if (configValue == null || configValue.getValue() == null) {
            return null;
        }

        // Avoid extra StringBuilder allocations from Expression
        if (configValue.getValue().indexOf('$') == -1) {
            return configValue;
        }

        ConfigValue.ConfigValueBuilder valueBuilder = configValue.from();
        Expression expression = Expression.compile(escapeDollarIfExists(configValue.getValue()), LENIENT_SYNTAX, NO_TRIM,
                NO_SMART_BRACES, DOUBLE_COLON);
        String expanded = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(ResolveContext<RuntimeException> resolveContext, StringBuilder stringBuilder) {
                String key = resolveContext.getKey();

                // Requires a handler lookup
                int index = key.indexOf("::");
                if (index != -1) {
                    stringBuilder.append(getHandler(key.substring(0, index), context).decode(key.substring(index + 2)));
                    return;
                }

                // Expression lookup
                ConfigValue resolve = getValue(context, key, depth + 1);
                if (resolve != null) {
                    if (!resolve.hasProblems()) {
                        stringBuilder.append(resolve.getValue());
                    } else {
                        valueBuilder.withProblems(resolve.getProblems());
                    }
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    valueBuilder.addProblem(new Problem(msg.expandingElementNotFound(key, configValue.getName())));
                }
            }
        });

        return valueBuilder.withValue(expanded).build();
    }

    /**
     * MicroProfile Config defines the backslash escape for dollar to retrieve the raw expression. We don't want to
     * turn {@link Expression.Flag#ESCAPES} on because it may break working configurations.
     * <br>
     * This will replace the expected escape in MicroProfile Config by the escape used in {@link Expression}, a double
     * dollar.
     */
    private String escapeDollarIfExists(final String value) {
        int index = value.indexOf("\\$");
        if (index != -1) {
            int start = 0;
            StringBuilder builder = new StringBuilder();
            while (index != -1) {
                builder.append(value, start, index).append("$$");
                start = index + 2;
                index = value.indexOf("\\$", start);
            }
            builder.append(value.substring(start));
            return builder.toString();
        }
        return value;
    }

    private SecretKeysHandler getHandler(final String handlerName, final ConfigSourceInterceptorContext context) {
        SecretKeysHandler handler = handlers.get(handlerName);
        if (handler == null) {
            throw ConfigMessages.msg.secretKeyHandlerNotFound(handlerName);
        }

        if (handler instanceof SecretKeysHandlerFactory.LazySecretKeysHandler) {
            handler = ((SecretKeysHandlerFactory.LazySecretKeysHandler) handler).get(new ConfigSourceContext() {
                @Override
                public ConfigValue getValue(final String name) {
                    return new ExpressionConfigSourceInterceptor().getValue(context, name);
                }

                @Override
                public Iterator<String> iterateNames() {
                    return context.iterateNames();
                }
            });
        }
        return handler;
    }
}
