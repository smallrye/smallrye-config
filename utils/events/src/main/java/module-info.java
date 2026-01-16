module io.smallrye.config.events {
    requires jakarta.annotation;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.interceptor;

    requires org.jboss.logging;
    requires org.jboss.logging.annotations;

    exports io.smallrye.config.events;
    exports io.smallrye.config.events.regex;
}