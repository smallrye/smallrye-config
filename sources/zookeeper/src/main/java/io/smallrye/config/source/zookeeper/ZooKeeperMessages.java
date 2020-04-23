package io.smallrye.config.source.zookeeper;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCFG", length = 5)
interface ZooKeeperMessages {
    ZooKeeperMessages msg = Messages.getBundle(ZooKeeperMessages.class);

    @Message(id = 4000, value = "Please set properties for \"" +
            ZooKeeperConfigSource.ZOOKEEPER_URL_KEY + "\" and \"" +
            ZooKeeperConfigSource.APPLICATION_ID_KEY + "\"")
    ZooKeeperConfigException propertiesNotSet();
}
