package io.smallrye.config;

import java.io.InvalidObjectException;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCFG", length = 5)
interface ConfigMessages {
    ConfigMessages msg = Messages.getBundle(ConfigMessages.class);

    @Message(id = 0, value = "The file %s was not found")
    IllegalStateException fileNotFound(String fileName);

    @Message(id = 1, value = "Failure while loading microprofile-config.properties files")
    IllegalStateException failedToLoadConfig(@Cause Throwable throwable);

    @Message(id = 2, value = "%s can not be converted to a Character")
    IllegalArgumentException failedCharacterConversion(String value);

    @Message(id = 3, value = "Converter %s must be parameterized with a single type")
    IllegalStateException singleTypeConverter(String className);

    @Message(id = 4, value = "%s is not an array type")
    IllegalArgumentException notArrayType(String arrayType);

    @Message(id = 5, value = "Value does not match pattern %s (value was \"%s\")")
    IllegalArgumentException valueNotMatchPattern(Pattern pattern, String value);

    @Message(id = 6, value = "Value must not be less than %s (value was \"%s\")")
    IllegalArgumentException lessThanMinimumValue(Object minimum, String value);

    @Message(id = 7, value = "Value must not be less than or equal to %s (value was \"%s\")")
    IllegalArgumentException lessThanEqualToMinimumValue(Object minimum, String value);

    @Message(id = 8, value = "Value must not be greater than %s (value was \"%s\")")
    IllegalArgumentException greaterThanMaximumValue(Object maximum, String value);

    @Message(id = 9, value = "Value must not be greater than or equal to %s (value was \"%s\")")
    IllegalArgumentException greaterThanEqualToMaximumValue(Object maximum, String value);

    @Message(id = 10, value = "Unknown converter ID: %s")
    InvalidObjectException unknownConverterId(int id);

    @Message(id = 11, value = "Could not expand value %s in property %s")
    NoSuchElementException expandingElementNotFound(String key, String valueName);

    @Message(id = 12, value = "Can not add converter %s that is not parameterized with a type")
    IllegalStateException unableToAddConverter(Converter<?> converter);

    @Message(id = 13, value = "No Converter registered for %s")
    IllegalArgumentException noRegisteredConverter(Class<?> type);

    @Message(id = 14, value = "Property %s not found")
    NoSuchElementException propertyNotFound(String name);

    @Message(id = 15, value = "No configuration is available for this class loader")
    IllegalStateException noConfigForClassloader();

    @Message(id = 16, value = "config cannot be null")
    IllegalArgumentException configIsNull();

    @Message(id = 17, value = "Configuration already registered for the given class loader")
    IllegalStateException configAlreadyRegistered();

    @Message(id = 18, value = "Malformed \\uxxxx encoding")
    IllegalArgumentException malformedEncoding();

    @Message(id = 19, value = "Failed to create new instance from Converter constructor")
    IllegalArgumentException constructorConverterFailure(@Cause Throwable cause);

    @Message(id = 20, value = "Failed to convert value with static method")
    IllegalArgumentException staticMethodConverterFailure(@Cause Throwable cause);

    @Message(id = 21, value = "Converter class %s not found")
    IllegalArgumentException classConverterNotFound(@Cause Throwable cause, String className);

    @Message(id = 22, value = "Host, %s, not found")
    IllegalArgumentException unknownHost(@Cause Throwable cause, String host);

    @Message(id = 23, value = "Array type being converted is unknown")
    IllegalArgumentException unknownArrayType();

    @Message(id = 24, value = "Not allowed to access secret key %s")
    SecurityException notAllowed(String name);

    @Message(id = 25, value = "Recursive expression expansion is too deep for %s")
    IllegalArgumentException expressionExpansionTooDepth(String name);

    @Message(id = 26, value = "%s cannot be converted into a UUID")
    IllegalArgumentException malformedUUID(@Cause Throwable cause, String malformedUUID);

    @Message(id = 27, value = "Could not find a mapping for %s with prefix %s")
    NoSuchElementException mappingNotFound(String className, String prefix);

    @Message(id = 28, value = "Type %s not supported for unwrapping.")
    IllegalArgumentException getTypeNotSupportedForUnwrapping(Class<?> type);
}
