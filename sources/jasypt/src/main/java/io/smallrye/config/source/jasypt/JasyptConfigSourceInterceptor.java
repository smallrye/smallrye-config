package io.smallrye.config.source.jasypt;

import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_SMART_BRACES;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.util.function.BiConsumer;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.jasypt.properties.PropertyValueEncryptionUtils;

import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;
import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

public class JasyptConfigSourceInterceptor implements ConfigSourceInterceptor {
    private static final long serialVersionUID = 4525651736553718297L;

    private final StringEncryptor encryptor;

    public JasyptConfigSourceInterceptor(final JasyptConfig jasyptConfig) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(jasyptConfig.encryptor().password());
        encryptor.setAlgorithm(jasyptConfig.encryptor().algorithm());
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.initialize();
        this.encryptor = encryptor;
    }

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
                if ("jasypt".equals(key) && resolveContext.hasDefault()) {
                    String encoded = resolveContext.getExpandedDefault();
                    String decrypt = PropertyValueEncryptionUtils.decrypt(encoded, encryptor);
                    stringBuilder.append(decrypt);
                } else {
                    stringBuilder.append(configValue.getValue());
                }
            }
        });

        return configValue.withValue(expanded);
    }
}
