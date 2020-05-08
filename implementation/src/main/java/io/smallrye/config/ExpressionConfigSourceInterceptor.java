package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import javax.annotation.Priority;

import io.smallrye.common.expression.Expression;

@Priority(Priorities.LIBRARY + 600)
public class ExpressionConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -539336551011916218L;

    private static final int MAX_DEPTH = 32;

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return getValue(context, name, 1);
    }

    private ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name, final int depth) {
        if (depth == MAX_DEPTH) {
            throw ConfigMessages.msg.expressionExpansionTooDepth(name);
        }

        final ConfigValue configValue = context.proceed(name);

        if (!Expressions.isEnabled()) {
            return configValue;
        }

        if (configValue == null) {
            return null;
        }

        final Expression expression = Expression.compile(configValue.getValue(), LENIENT_SYNTAX, NO_TRIM);
        final String expanded = expression.evaluate((resolveContext, stringBuilder) -> {
            final ConfigValue resolve = getValue(context, resolveContext.getKey(), depth + 1);
            if (resolve != null) {
                stringBuilder.append(resolve.getValue());
            } else if (resolveContext.hasDefault()) {
                resolveContext.expandDefault();
            } else {
                throw ConfigMessages.msg.expandingElementNotFound(resolveContext.getKey(), configValue.getName());
            }
        });

        return configValue.withValue(expanded);
    }
}
