package io.smallrye.config.examples.profiles;

import io.smallrye.config.Config;

public class ExampleProfiles {
    public static String getMyProp() {
        return Config.getOrCreate().getValue("my.prop", String.class);
    }
}
