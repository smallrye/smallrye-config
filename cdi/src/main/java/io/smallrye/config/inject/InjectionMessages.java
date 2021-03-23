package io.smallrye.config.inject;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;

@MessageBundle(projectCode = "SRCFG", length = 5)
public interface InjectionMessages {
    InjectionMessages msg = Messages.getBundle(InjectionMessages.class);

    @Message(id = 2000, value = "Failed to Inject @ConfigProperty for key %s into %s since the config property could not be found in any config source")
    ConfigInjectionException noConfigValue(String key, String location, @Param String configPropertyName);

    @Message(id = 2001, value = "Failed to Inject @ConfigProperty for key %s into %s %s")
    ConfigInjectionException retrieveConfigFailure(String key, String location, String causeMessage,
            @Param String configPropertyName, @Cause Exception e);

    @Message(id = 2002, value = "Could not find default name for @ConfigProperty InjectionPoint %s")
    IllegalStateException noConfigPropertyDefaultName(InjectionPoint injectionPoint);

    @Message(id = 2003, value = "Failed to create @ConfigProperties bean %s")
    ConfigInjectionException retrieveConfigPropertiesFailure(String causeMessage, @Cause Exception e);

    @Message(id = 2004, value = "Unhandled ConfigProperty")
    IllegalStateException unhandledConfigProperty();

    @Message(id = 2005, value = "Type has no raw type class: %s")
    IllegalArgumentException noRawType(Type type);

    @Message(id = 2006, value = "No Converter registered for %s")
    IllegalArgumentException noRegisteredConverter(Class<?> type);
}
