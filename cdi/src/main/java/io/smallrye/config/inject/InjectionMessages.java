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

    @Message(id = 2000, value = "Failed to Inject @ConfigProperty for key %s into %s since the config property could not be found in any config source")
    ConfigInjectionException noConfigValue(@Param @Pos(1) String configPropertyName, @Pos(2) String location);

    @Message(id = 2001, value = "Failed to Inject @ConfigProperty for key %s into %s %s")
    ConfigInjectionException retrieveConfigFailure(@Param @Pos(1) String configPropertyName, @Pos(2) String location,
            @Pos(3) String causeMessage, @Cause Exception e);

    @Message(id = 2002, value = "Could not find default name for @ConfigProperty InjectionPoint %s")
    IllegalStateException noConfigPropertyDefaultName(InjectionPoint injectionPoint);

    @Message(id = 2003, value = "Failed to create @ConfigProperties bean with prefix %s for class %s. %s")
    ConfigInjectionException retrieveConfigPropertiesFailure(String prefix, String className, String causeMessage,
            @Cause Exception e);

    @Message(id = 2004, value = "Failed to create @ConfigMapping bean with prefix %s for interface %s. %s")
    ConfigInjectionException retrieveConfigMappingsFailure(String prefix, String interfaceName, String causeMessage,
            @Cause Exception e);

    @Message(id = 2005, value = "Unhandled ConfigProperty")
    IllegalStateException unhandledConfigProperty();

    @Message(id = 2006, value = "Type has no raw type class: %s")
    IllegalArgumentException noRawType(Type type);

    @Message(id = 2007, value = "No Converter registered for %s")
    IllegalArgumentException noRegisteredConverter(Class<?> type);

}
