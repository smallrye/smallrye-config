package io.smallrye.config.source.filevault;

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.Priorities;

public class FileVaultConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        return new FileVaultConfigSourceInterceptor();
    }

    @Override
    public OptionalInt getPriority() {
        // So it evaluates before ExpressionConfigSourceInterceptor
        return OptionalInt.of(Priorities.LIBRARY + 300 - 1);
    }
}
