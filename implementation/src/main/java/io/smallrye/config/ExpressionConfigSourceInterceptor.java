package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.DOUBLE_COLON;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;
import static io.smallrye.config.ConfigMessages.msg;

import java.util.function.BiConsumer;

import jakarta.annotation.Priority;

import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;
import io.smallrye.config.ConfigValidationException.Problem;

@Priority(Priorities.LIBRARY + 300)
public class ExpressionConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -539336551011916218L;

    private static final int MAX_DEPTH = 32;

    private final boolean enabled;

    public ExpressionConfigSourceInterceptor() {
        this(true);
    }

    public ExpressionConfigSourceInterceptor(final boolean enabled) {
        this.enabled = enabled;
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

        ConfigValue.ConfigValueBuilder value = configValue.from();
        Expression expression = Expression.compile(escapeDollarIfExists(configValue.getValue()), LENIENT_SYNTAX, NO_TRIM,
                NO_SMART_BRACES, DOUBLE_COLON);
        String expanded = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(ResolveContext<RuntimeException> resolveContext, StringBuilder stringBuilder) {
                String key = resolveContext.getKey();

                // Requires a handler lookup
                int index = key.indexOf("::");
                if (index != -1) {
                    value.withExtendedExpressionHandler(key.substring(0, index));
                    stringBuilder.append(key, index + 2, key.length());
                    return;
                }

                // Expression lookup
                ConfigValue resolve = getValue(context, key, depth + 1);
                if (resolve != null) {
                    if (resolve.getProblems().isEmpty()) {
                        stringBuilder.append(resolve.getValue());
                    } else {
                        value.withProblems(resolve.getProblems());
                    }
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    value.addProblem(new Problem(msg.expandingElementNotFound(key, configValue.getName())));
                }
            }
        });

        return value.withValue(expanded).build();
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
}
