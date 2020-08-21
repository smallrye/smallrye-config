package io.smallrye.config.examples.mapping;

import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.config.SmallRyeConfig;

public class ServerMapping {
    public static Server getServer() {
        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        return config.getConfigMapping(Server.class);
    }
}
