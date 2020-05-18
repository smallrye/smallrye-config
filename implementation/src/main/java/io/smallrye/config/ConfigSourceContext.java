package io.smallrye.config;

import io.smallrye.common.annotation.Experimental;

@Experimental("")
public interface ConfigSourceContext {
    ConfigValue getValue(String name);
}
