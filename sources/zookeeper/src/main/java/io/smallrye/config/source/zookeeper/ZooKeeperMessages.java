package io.smallrye.config.source.zookeeper;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCFG", length = 5)
interface ZooKeeperMessages {
    ZooKeeperMessages msg = Messages.getBundle(MethodHandles.lookup(), ZooKeeperMessages.class);

    @Message(id = 4000, value = "Please set properties for \"" +
            ZooKeeperConfigSource.ZOOKEEPER_URL_KEY + "\" and \"" +
            ZooKeeperConfigSource.APPLICATION_ID_KEY + "\"")
    ZooKeeperConfigException propertiesNotSet();
}
