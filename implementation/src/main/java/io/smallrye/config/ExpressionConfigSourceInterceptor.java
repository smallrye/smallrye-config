package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import javax.annotation.Priority;

import io.smallrye.common.expression.Expression;

@Priority(500)
public class ExpressionConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final ThreadLocal<Boolean> ENABLE = ThreadLocal.withInitial(() -> Boolean.TRUE);

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final ConfigValue configValue = context.proceed(name);

        if (!ENABLE.get()) {
            return configValue;
        }

        final Expression expression = Expression.compile(configValue.getValue(), LENIENT_SYNTAX, NO_TRIM);
        final String expanded = expression.evaluate((resolveContext, stringBuilder) -> {
            final ConfigValue resolve = context.proceed(resolveContext.getKey());
            if (resolve != null) {
                stringBuilder.append(resolve.getValue());
            } else if (resolveContext.hasDefault()) {
                resolveContext.expandDefault();
            }
        });

        return configValue.withValue(expanded);
    }

    static boolean enable() {
        try {
            return ENABLE.get() == Boolean.FALSE;
        } finally {
            ENABLE.set(Boolean.TRUE);
        }
    }

    static boolean disable() {
        try {
            return ENABLE.get() == Boolean.TRUE;
        } finally {
            ENABLE.set(Boolean.FALSE);
        }
    }
}
