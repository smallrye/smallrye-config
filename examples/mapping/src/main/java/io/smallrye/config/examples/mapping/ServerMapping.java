package io.smallrye.config.examples.mapping;

import io.smallrye.config.Config;

public class ServerMapping {
    public static Server getServer() {
        return Config.getOrCreate().getConfigMapping(Server.class);
    }
}
