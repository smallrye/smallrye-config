package io.smallrye.config.examples.expansion;

import org.eclipse.microprofile.config.ConfigProvider;

public class ExampleExpansion {
    public static String getMyProp() {
        return ConfigProvider.getConfig().getValue("my.prop", String.class);
    }
}
