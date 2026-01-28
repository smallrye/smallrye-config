package io.smallrye.config.examples.expansion;

import io.smallrye.config.Config;

public class ExampleExpansion {
    public static String getMyProp() {
        return Config.getOrCreate().getValue("my.prop", String.class);
    }
}
