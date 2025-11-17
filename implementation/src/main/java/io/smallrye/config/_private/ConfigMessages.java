package io.smallrye.config._private;

import java.io.InvalidObjectException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "SRCFG", length = 5)
public interface ConfigMessages {
    ConfigMessages msg = Messages.getBundle(MethodHandles.lookup(), ConfigMessages.class);

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

    // Returns a String rather than a NoSuchElementException for a slight performance improvement as throwing this exception could be quite common.
    @Message(id = 14, value = "The config property %s is required but it could not be found in any config source")
    String propertyNotFound(String name);

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

    @Message(id = 27, value = "Could not find a mapping for %s")
    NoSuchElementException mappingNotFound(String className);

    @Message(id = 28, value = "Could not find a mapping for %s with prefix %s")
    NoSuchElementException mappingPrefixNotFound(String className, String prefix);

    @Message(id = 29, value = "Expected an integer value, got \"%s\"")
    NumberFormatException integerExpected(String value);

    @Message(id = 30, value = "Expected a long value, got \"%s\"")
    NumberFormatException longExpected(String value);

    @Message(id = 31, value = "Expected a double value, got \"%s\"")
    NumberFormatException doubleExpected(String value);

    @Message(id = 32, value = "Expected a float value, got \"%s\"")
    NumberFormatException floatExpected(String value);

    @Message(id = 33, value = "Scheme %s not supported")
    IllegalArgumentException schemeNotSupported(String scheme);

    @Message(id = 34, value = "URI Syntax invalid %s")
    IllegalArgumentException uriSyntaxInvalid(@Cause Throwable cause, String uri);

    @Message(id = 35, value = "Failed to load resource %s")
    IllegalArgumentException failedToLoadResource(@Cause Throwable cause, String location);

    @Message(id = 36, value = "Type %s not supported for unwrapping.")
    IllegalArgumentException getTypeNotSupportedForUnwrapping(Class<?> type);

    @Message(id = 37, value = "The Converter API cannot convert a null value")
    NullPointerException converterNullValue();

    @Message(id = 38, value = "Type has no raw type class: %s")
    IllegalArgumentException noRawType(Type type);

    @Message(id = 39, value = "The config property %s with the config value \"%s\" threw an Exception whilst being converted %s")
    IllegalArgumentException converterException(@Cause Throwable converterException, String configProperty, String configValue,
            String causeMessage);

    @Message(id = 40, value = "The config property %s is defined as the empty String (\"\") which the following Converter considered to be null: %s")
    NoSuchElementException propertyEmptyString(String configPropertyName, String converter);

    @Message(id = 41, value = "The config property %s with the config value \"%s\" was converted to null from the following Converter: %s")
    NoSuchElementException converterReturnedNull(String configPropertyName, String configValue, String converter);

    @Message(id = 42, value = "Value does not match the expected map format \"<key1>=<value1>;<key2>=<value2>...\" (value was \"%s\")")
    NoSuchElementException valueNotMatchMapFormat(String value);

    @Message(id = 43, value = "The @ConfigMapping annotation can only be placed in interfaces, %s is a class")
    IllegalStateException mappingAnnotationNotSupportedInClass(Class<?> type);

    @Message(id = 44, value = "The @ConfigProperties annotation can only be placed in classes, %s is an interface")
    IllegalStateException propertiesAnnotationNotSupportedInInterface(Class<?> type);

    @Message(id = 45, value = "The %s class is not a ConfigMapping")
    IllegalArgumentException classIsNotAMapping(Class<?> type);

    @Message(id = 46, value = "Could not find a secret key handler for %s")
    NoSuchElementException secretKeyHandlerNotFound(String handler);

    @Message(id = 47, value = "The ConfigMapping path %s is ambiguous. It is mapped by %s and %s")
    IllegalStateException ambiguousMapping(String path, String amb1, String amb2);

    @Message(id = 48, value = "The config property %s explicitly defined the key %s, but the key is marked as unnamed")
    IllegalArgumentException explicitNameInUnnamed(String name, String key);

    @Message(id = 49, value = "Cannot convert %s to enum %s, allowed values: %s")
    IllegalArgumentException cannotConvertEnum(String value, Class<?> enumType, String values);

    @Message(id = 50, value = "%s in %s does not map to any root")
    IllegalStateException propertyDoesNotMapToAnyRoot(String name, String location);

    @Message(id = 51, value = "Could not generate ConfigMapping %s")
    IllegalStateException couldNotGenerateMapping(@Cause Throwable throwable, String mapping);

    @Message(id = 52, value = "@ConfigMapping methods cannot accept parameters: %s")
    IllegalArgumentException mappingMethodsCannotAcceptParameters(String method);

    @Message(id = 53, value = "@ConfigMapping methods cannot be void: %s")
    IllegalArgumentException mappingMethodsCannotBeVoid(String method);

    @Message(id = 54, value = "@ConfigMapping cannot use self-reference types: %s")
    IllegalArgumentException mappingCannotUseSelfReferenceTypes(String type);
}
