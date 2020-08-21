package io.smallrye.config.examples.mapping;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ServerMappingBean {
    @Inject
    Server server;

    public Server getServer() {
        return server;
    }
}
