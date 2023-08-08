package io.smallrye.config.test.builder;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class CustomOneConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("one", "one").addDefaultSources();
    }

    @Override
    public int priority() {
        return 1;
    }
}
