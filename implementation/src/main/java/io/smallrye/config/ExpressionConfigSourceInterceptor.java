package io.smallrye.config;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;
import static io.smallrye.config.ConfigMessages.msg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.Config;

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
        if (depth == MAX_DEPTH) {
            throw msg.expressionExpansionTooDepth(name);
        }

        ConfigValue configValue = context.proceed(name);

        if (!Expressions.isEnabled() || !enabled) {
            return configValue;
        }

        if (configValue == null) {
            return null;
        }

        List<Problem> problems = new ArrayList<>();
        Expression expression = Expression.compile(escapeDollarIfExists(configValue.getValue()), LENIENT_SYNTAX, NO_TRIM,
                NO_SMART_BRACES);
        String expanded = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(ResolveContext<RuntimeException> resolveContext, StringBuilder stringBuilder) {
                ConfigValue resolve = getValue(context, resolveContext.getKey(), depth + 1);
                if (resolve != null) {
                    stringBuilder.append(resolve.getValue());
                    problems.addAll(resolve.getProblems());
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    problems.add(new Problem(msg.expandingElementNotFound(resolveContext.getKey(), configValue.getName())));
                }
            }
        });

        return problems.isEmpty() ? configValue.withValue(expanded) : configValue.withValue(null).withProblems(problems);
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
