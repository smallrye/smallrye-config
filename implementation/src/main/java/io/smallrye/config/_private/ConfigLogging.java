package io.smallrye.config._private;

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

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 1003, value = "Unable to get declared constructor for class %s with arguments %s")
    void failedToRetrieveDeclaredConstructor(@Cause Throwable cause, String clazz, String paramTypes);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 1004, value = "Unable to set accessible flag on %s")
    void failedToSetAccessible(@Cause Throwable cause, String accessibleObject);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 1005, value = "Could not find sources with %s in %s")
    void configLocationsNotFound(String name, String value);
}
