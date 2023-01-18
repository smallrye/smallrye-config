package io.smallrye.config.examples.mapping;

import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.config.SmallRyeConfig;

public class ServerMapping {
    public static Server getServer() {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        return config.getConfigMapping(Server.class);
    }
}
