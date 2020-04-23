package io.smallrye.config.source.file;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "SRCFG", length = 5)
interface FileSystemLogging extends BasicLogger {
    FileSystemLogging log = Logger.getMessageLogger(FileSystemLogging.class, FileSystemLogging.class.getPackage().getName());

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 3000, value = "Unable to read content from file %s. Exception: %s")
    void failedToReadFileContent(String file, String cause);
}
