package io.smallrye.config.events.regex;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCFG", length = 5)
interface RegexLogging extends BasicLogger {

    // if we add message localization one day, we must drop the Locale.ROOT argument
    RegexLogging log = Logger.getMessageLogger(MethodHandles.lookup(), RegexLogging.class,
            RegexLogging.class.getPackage().getName(), Locale.ROOT);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 5000, value = "Can not find ChangeEvent parameter for method %s. @RegexFilter is being ignored")
    void changeEventParameterMissing(String methodName);
}
