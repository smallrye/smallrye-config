package io.smallrye.config.inject;

import java.lang.reflect.Type;
import java.util.NoSuchElementException;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.Pos;

@MessageBundle(projectCode = "SRCFG", length = 5)
public interface InjectionMessages {
    InjectionMessages msg = Messages.getBundle(InjectionMessages.class);

    @Message(id = 2000, value = "No Config Value exists for required property %s")
    ConfigException noConfigValue(@Param @Pos(1) String key);

    @Message(id = 2001, value = "Failed to retrieve config for key %s")
    ConfigException retrieveConfigFailure(@Cause IllegalArgumentException cause, @Param @Pos(1) String key);

    @Message(id = 2002, value = "Could not find default name for @ConfigProperty InjectionPoint %s")
    IllegalStateException noConfigPropertyDefaultName(InjectionPoint injectionPoint);

    @Message(id = 2003, value = "Unhandled ConfigProperty")
    IllegalStateException unhandledConfigProperty();

    @Message(id = 2004, value = "Required property %s not found")
    NoSuchElementException propertyNotFound(String name);

    @Message(id = 2005, value = "Type has no raw type class: %s")
    IllegalArgumentException noRawType(Type type);
}
