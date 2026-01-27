package io.smallrye.config.source.zookeeper;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCFG", length = 5)
interface ZooKeepperLogging extends BasicLogger {

    // if we add message localization one day, we must drop the Locale.ROOT argument
    ZooKeepperLogging log = Logger.getMessageLogger(MethodHandles.lookup(), ZooKeepperLogging.class,
            ZooKeepperLogging.class.getPackage().getName(), Locale.ROOT);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 4500, value = "Failed to retrieve property names from ZooKeeperConfigSource")
    void failedToRetrievePropertyNames(@Cause Throwable throwable);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 4501, value = "Failed to retrieve properties from ZooKeeperConfigSource")
    void failedToRetrieveProperties(@Cause Throwable throwable);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 4502, value = "Failed to retrieve property value for %s from ZooKeeperConfigSource")
    void failedToRetrieveValue(@Cause Throwable throwable, String key);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 4503, value = "Configuring ZooKeeperConfigSource using url: %s, and applicationId: %s")
    void configuringZookeeper(String url, String applicationId);
}
