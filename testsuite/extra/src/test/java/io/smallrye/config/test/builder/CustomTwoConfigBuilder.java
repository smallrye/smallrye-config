package io.smallrye.config.test.builder;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class CustomTwoConfigBuilder implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("one", "two");
        if (builder.isAddDefaultSources()) {
            builder.withDefaultValue("addDefaultSources", "true");
        }
    }

    @Override
    public int priority() {
        return 2;
    }
}
