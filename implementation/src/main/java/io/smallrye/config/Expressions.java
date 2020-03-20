package io.smallrye.config;

import static io.smallrye.config.ExpressionConfigSourceInterceptor.disable;
import static io.smallrye.config.ExpressionConfigSourceInterceptor.enable;

public class Expressions {
    public static void withoutExpansion(final Runnable action) {
        if (disable()) {
            try {
                action.run();
            } finally {
                enable();
            }
        } else {
            action.run();
        }
    }
}
