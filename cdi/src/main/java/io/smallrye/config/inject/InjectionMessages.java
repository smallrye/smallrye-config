package io.smallrye.config.inject;

import java.lang.reflect.Type;

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

    @Message(id = 2000, value = "Failed to Inject @ConfigProperty for key %s since the config property could not be found in any config source")
    ConfigInjectionException noConfigValue(@Param @Pos(1) String key);

    @Message(id = 2001, value = "Failed to Inject @ConfigProperty for key %s %s")
    ConfigInjectionException retrieveConfigFailure(@Param @Pos(1) String key, @Param @Pos(2) String cuase, @Cause Exception e);

    @Message(id = 2002, value = "Could not find default name for @ConfigProperty InjectionPoint %s")
    IllegalStateException noConfigPropertyDefaultName(InjectionPoint injectionPoint);

    @Message(id = 2003, value = "Failed to create @ConfigProperties bean %s")
    ConfigInjectionException retrieveConfigPropertiesFailure(@Param @Pos(1) String cuase, @Cause Exception e);

    @Message(id = 2004, value = "Unhandled ConfigProperty")
    IllegalStateException unhandledConfigProperty();

    @Message(id = 2005, value = "Type has no raw type class: %s")
    IllegalArgumentException noRawType(Type type);

    @Message(id = 2006, value = "No Converter registered for %s")
    IllegalArgumentException noRegisteredConverter(Class<?> type);
}
