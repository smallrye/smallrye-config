package io.smallrye.config.examples.profiles;

import org.eclipse.microprofile.config.ConfigProvider;

public class ExampleProfiles {
    public static String getMyProp() {
        return ConfigProvider.getConfig().getValue("my.prop", String.class);
    }
}
