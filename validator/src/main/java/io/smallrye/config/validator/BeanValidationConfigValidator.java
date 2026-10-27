package io.smallrye.config.validator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;

import io.smallrye.config.ConfigMappingHandler;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.Property;
import io.smallrye.config.ConfigMappingLoader.GeneratedConfigClass;
import io.smallrye.config.ConfigValidationException;
import io.smallrye.config.ConfigValidationException.Problem;
import io.smallrye.config.ConfigValidator;

public interface BeanValidationConfigValidator extends ConfigValidator {

    Validator getValidator();

    @Override
    default void validateMapping(
            final GeneratedConfigClass configClass,
            final Object configObject)
            throws ConfigValidationException {

        List<Problem> problems = new ArrayList<>();
        if (configClass.getParent().equals(configClass.getInterfaceType())) {
            ConfigMappingHandler handler = configClass.getHandler();
            Class<?> type = configClass.getInterfaceType();
            validateMappingInterface(
                    configClass,
                    handler.getPrefix(type),
                    handler.getBeanStyleGetters(type).andThen(handler.getNamingStrategy(type)),
                    configObject,
                    problems);
        } else {
            validateMappingClass(
                    configClass,
                    configObject,
                    configClass.getHandler().getPrefix(configClass.getParent()),
                    problems);
        }

        if (!problems.isEmpty()) {
            throw new ConfigValidationException(problems.toArray(ConfigValidationException.Problem.NO_PROBLEMS));
        }
    }

    default void validateMappingInterface(
            final GeneratedConfigClass configClass,
            final String currentPath,
            final Function<String, String> namingStrategy,
            final Object configObject,
            final List<Problem> problems) {

        for (Property property : configClass.getProperties()) {
            validateProperty(property, currentPath, namingStrategy, configObject, false, problems);
        }

        validateMappingClass(configClass, configObject, currentPath, problems);
    }

    default void validateProperty(
            final Property property,
            final String currentPath,
            final Function<String, String> namingStrategy,
            final Object configObject,
            final boolean optional,
            final List<Problem> problems) {

        if (property.isOptional()) {
            validateProperty(property.asOptional().getNestedProperty(), currentPath, namingStrategy, configObject, true,
                    problems);
        }

        if ((property.isLeaf() || property.isPrimitive()) && !property.isOptional()) {
            validatePropertyValue(property, currentPath, namingStrategy, configObject, problems);
        }

        if (property.isGroup()) {
            try {
                Object group = property.getMethod().invoke(configObject);
                // unwrap
                if (optional) {
                    Optional<?> optionalGroup = (Optional<?>) group;
                    if (optionalGroup.isEmpty()) {
                        return;
                    }
                    group = optionalGroup.get();
                }

                validatePropertyValue(property, currentPath, namingStrategy, configObject, problems);
                validateMappingInterface(property.asGroup().getGroupType(),
                        appendPropertyName(currentPath, namingStrategy, property),
                        namingStrategy, group, problems);
            } catch (IllegalAccessException e) {
                throw new IllegalAccessError(e.getMessage());
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (RuntimeException | Error e2) {
                    throw e2;
                } catch (Throwable t2) {
                    throw new UndeclaredThrowableException(t2);
                }
            }
        }

        if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            if (collectionProperty.getElement().isGroup()) {
                try {
                    Object object = property.getMethod().invoke(configObject);
                    if (optional) {
                        Optional<?> optionalCollection = (Optional<?>) object;
                        if (optionalCollection.isEmpty()) {
                            return;
                        }
                        object = optionalCollection.get();
                    }
                    Collection<?> collection = (Collection<?>) object;
                    int i = 0;
                    for (Object element : collection) {
                        validateMappingInterface(collectionProperty.getElement().asGroup().getGroupType(),
                                appendPropertyName(currentPath, namingStrategy, property) + "[" + i + "]",
                                namingStrategy, element, problems);
                        i++;
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (InvocationTargetException e) {
                    try {
                        throw e.getCause();
                    } catch (RuntimeException | Error e2) {
                        throw e2;
                    } catch (Throwable t2) {
                        throw new UndeclaredThrowableException(t2);
                    }
                }
            }
            validatePropertyValue(property, currentPath, namingStrategy, configObject, problems);
        }

        if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            if (mapProperty.getValueProperty().isGroup()) {
                try {
                    Map<?, ?> map = (Map<?, ?>) property.getMethod().invoke(configObject);
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        validateMappingInterface(mapProperty.getValueProperty().asGroup().getGroupType(),
                                appendPropertyName(currentPath, namingStrategy, property) + "." + entry.getKey(),
                                namingStrategy, entry.getValue(), problems);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (InvocationTargetException e) {
                    try {
                        throw e.getCause();
                    } catch (RuntimeException | Error e2) {
                        throw e2;
                    } catch (Throwable t2) {
                        throw new UndeclaredThrowableException(t2);
                    }
                }
            } else if (mapProperty.getValueProperty().isCollection()) {
                try {
                    CollectionProperty collectionProperty = mapProperty.getValueProperty().asCollection();
                    if (collectionProperty.getElement().isGroup()) {
                        Map<?, ?> map = (Map<?, ?>) property.getMethod().invoke(configObject);
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            Collection<?> elements = (Collection<?>) entry.getValue();
                            int i = 0;
                            for (Object element : elements) {
                                validateMappingInterface(collectionProperty.getElement().asGroup().getGroupType(),
                                        appendPropertyName(currentPath, namingStrategy, property) + "." + entry.getKey() + "["
                                                + i + "]",
                                        namingStrategy, element, problems);
                                i++;
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessError(e.getMessage());
                } catch (InvocationTargetException e) {
                    try {
                        throw e.getCause();
                    } catch (RuntimeException | Error e2) {
                        throw e2;
                    } catch (Throwable t2) {
                        throw new UndeclaredThrowableException(t2);
                    }
                }
            }
            validatePropertyValue(property, currentPath, namingStrategy, configObject, problems);
        }
    }

    default void validatePropertyValue(
            final Property property,
            final String currentPath,
            final Function<String, String> namingStrategy,
            final Object configObject,
            final List<Problem> problems) {

        try {
            Method methodToInvoke;
            if (property.getMethod().canAccess(configObject)) {
                methodToInvoke = property.getMethod();
            } else {
                try {
                    methodToInvoke = configObject.getClass().getMethod(property.getMethod().getName());
                } catch (NoSuchMethodException e) {
                    // This never happens, because we generated the class, and we know the method exists
                    throw new RuntimeException(e);
                }
            }

            Set<ConstraintViolation<Object>> violations = getValidator().forExecutables().validateReturnValue(configObject,
                    property.getMethod(), methodToInvoke.invoke(configObject));
            for (ConstraintViolation<Object> violation : violations) {
                problems.add(new Problem(interpolateMessage(currentPath, namingStrategy, property, violation)));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t2) {
                throw new UndeclaredThrowableException(t2);
            }
        }
    }

    default void validateMappingClass(
            final GeneratedConfigClass configClass,
            final Object configObject,
            final String currentPath,
            final List<Problem> problems) {

        // Mapped properties of interface mappings were already validated by validatePropertyValue. Bean Validation
        // treats bean-style getters (getFoo, isFoo, hasFoo) as bean properties, so their constraints would be
        // reported again, with the Java property name, by the type-level validation. Skip them, and only report
        // type-level constraints and non-mapped members, like default methods. Class mappings have no properties
        // to skip, since their constraints are only validated here.
        Set<String> validated = new HashSet<>();
        if (configClass.getParent().equals(configClass.getInterfaceType())) {
            for (Property property : configClass.getProperties()) {
                String name = property.getMethod().getName();
                validated.add(name);
                for (String prefix : new String[] { "get", "is", "has" }) {
                    if (name.length() > prefix.length() && name.startsWith(prefix)) {
                        validated
                                .add(Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1));
                    }
                }
            }
        }

        final Set<ConstraintViolation<Object>> violations = getValidator().validate(configObject);
        for (ConstraintViolation<Object> violation : violations) {
            Path propertyPath = violation.getPropertyPath();
            if (propertyPath.toString().isEmpty()) {
                problems.add(new Problem(currentPath + " " + violation.getMessage()));
            } else if (!validated.contains(propertyPath.iterator().next().getName())) {
                problems.add(new Problem(currentPath + " " + propertyPath + " " + violation.getMessage()));
            }
        }
    }

    default String appendPropertyName(
            final String currentPath,
            final Function<String, String> namingStrategy,
            final Property property) {
        String name = property.getPropertyName(namingStrategy);
        if (currentPath.isEmpty()) {
            return name;
        }

        if (name.isEmpty()) {
            return currentPath;
        }

        return currentPath + "." + name;
    }

    default String interpolateMessage(
            final String currentPath,
            final Function<String, String> namingStrategy,
            final Property property,
            final ConstraintViolation<?> violation) {
        StringBuilder propertyName = new StringBuilder(currentPath);
        String name = property.getPropertyName(namingStrategy);
        if (!name.isEmpty()) {
            propertyName.append(".").append(name);
        }
        Path propertyPath = violation.getPropertyPath();
        for (Path.Node node : propertyPath) {
            if (node.isInIterable()) {
                if (node.getIndex() != null) {
                    propertyName.append("[").append(node.getIndex()).append("]");
                } else if (node.getKey() != null) {
                    propertyName.append(".").append(node.getKey());
                }
            }
        }
        return propertyName + " " + violation.getMessage();
    }
}
