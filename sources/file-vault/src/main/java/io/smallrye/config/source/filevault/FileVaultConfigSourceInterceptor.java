package io.smallrye.config.source.filevault;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.util.function.BiConsumer;

import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class FileVaultConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = -45605050209974890L;

    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        ConfigValue configValue = context.proceed(name);

        if (configValue == null) {
            return null;
        }

        Expression expression = Expression.compile(configValue.getValue(), LENIENT_SYNTAX, NO_TRIM, NO_SMART_BRACES);
        String expanded = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(
                    final ResolveContext<RuntimeException> resolveContext,
                    final StringBuilder stringBuilder) {
                String key = resolveContext.getKey();
                if ("file-vault".equals(key) && resolveContext.hasDefault()) {
                    String[] split = resolveContext.getExpandedDefault().split("/");
                    String keyStore = split[0];
                    String alias = split[1];

                    ConfigValue keyStoreEntry = context
                            .proceed("io.smallrye.config.file-vault." + keyStore + ".properties." + alias);
                    if (keyStoreEntry != null && keyStoreEntry.getValue() != null) {
                        stringBuilder.append(keyStoreEntry.getValue());
                    }
                } else {
                    stringBuilder.append(configValue.getValue());
                }
            }
        });

        return configValue.withValue(expanded);
    }
}
