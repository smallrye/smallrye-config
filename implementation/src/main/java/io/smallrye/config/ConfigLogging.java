package io.smallrye.config;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCFG", length = 5)
public interface ConfigLogging extends BasicLogger {
    ConfigLogging log = Logger.getMessageLogger(ConfigLogging.class, ConfigLogging.class.getPackage().getName());

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 1000, value = "Unable to get context classloader instance")
    void failedToRetrieveClassloader(@Cause Throwable cause);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 1001, value = "The config %s was loaded from %s with the value %s")
    void lookup(String name, String source, String value);

    @LogMessage(level = Logger.Level.DEBUG)
    @Message(id = 1002, value = "The config %s was not found")
    void notFound(String name);
}
