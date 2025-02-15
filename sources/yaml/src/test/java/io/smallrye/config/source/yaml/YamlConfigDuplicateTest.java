package io.smallrye.config.source.yaml;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class YamlConfigDuplicateTest {

    @Test
    void yamlConfigDuplicate() {

        // setup logger to capture messages
        LogMessageInterceptorHandler logCaptureHandler = new LogMessageInterceptorHandler();
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.addHandler(logCaptureHandler);

        // sample yaml with duplicate keys
        String yaml = "---\n" +
                "quarkus:\n" +
                "  banner:\n" +
                "    enabled: false\n" +
                "  banner:\n" +
                "    enabled: true";

        // read yaml configuration
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new YamlConfigSource("yaml", yaml))
                .build();

        // check current value of duplicate key (the last value is read, true)
        Assertions.assertTrue(config.getValue("quarkus.banner.enabled", Boolean.class));

        // check if the duplication warning has been logged
        Assertions.assertTrue(logCaptureHandler.containsLogMessage("duplicate keys found : banner"));

        // remove log capture handler
        rootLogger.removeHandler(logCaptureHandler);
    }

    /*
     * Logging handler used to test if a message has been really logged.
     */
    public static class LogMessageInterceptorHandler extends Handler {

        private Set<String> messages = new HashSet<>();

        @Override
        public void publish(LogRecord record) {
            // add log messages to a set
            this.messages.add(record.getMessage());
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }

        @Override
        public boolean isLoggable(LogRecord record) {
            return super.isLoggable(record);
        }

        public boolean containsLogMessage(String message) {
            // check if a message has been logged
            return this.messages.contains(message);
        }

    }

}
