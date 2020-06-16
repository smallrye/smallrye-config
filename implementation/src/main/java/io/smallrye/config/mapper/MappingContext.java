package io.smallrye.config.mapper;

import static io.smallrye.config.mapper.ConfigurationInterface.LeafProperty;
import static io.smallrye.config.mapper.ConfigurationInterface.MapProperty;
import static io.smallrye.config.mapper.ConfigurationInterface.PrimitiveProperty;
import static io.smallrye.config.mapper.ConfigurationInterface.Property;
import static io.smallrye.config.mapper.ConfigurationInterface.getConfigurationInterface;
import static io.smallrye.config.mapper.ConfigurationInterface.rawTypeOf;
import static io.smallrye.config.mapper.ConfigurationInterface.typeOfParameter;
import static io.smallrye.config.mapper.ConfigurationValidationException.Problem;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.SmallRyeConfig;

/**
 * A mapping context. This is used by generated classes during configuration mapping, and is released once the configuration
 * mapping has completed.
 */
public final class MappingContext {

    private final Map<Class<?>, Map<String, Map<Object, Object>>> enclosedThings = new IdentityHashMap<>();
    private final Map<Class<?>, Map<String, ConfigurationObject>> roots = new IdentityHashMap<>();
    private final Map<Class<?>, Map<String, Converter<?>>> convertersByTypeAndField = new IdentityHashMap<>();
    private final List<Map<Class<?>, Map<String, Converter<?>>>> keyConvertersByDegreeTypeAndField = new ArrayList<>();
    private final Map<Class<?>, Converter<?>> converterInstances = new IdentityHashMap<>();
    private final List<ConfigurationObject> allInstances = new ArrayList<>();
    private final SmallRyeConfig config;
    private final StringBuilder stringBuilder = new StringBuilder();
    private final ArrayList<Problem> problems = new ArrayList<>();

    MappingContext(final SmallRyeConfig config) {
        this.config = config;
    }

    public ConfigurationObject getRoot(Class<?> rootType, String rootPath) {
        return roots.getOrDefault(rootType, Collections.emptyMap()).get(rootPath);
    }

    public void registerRoot(Class<?> rootType, String rootPath, ConfigurationObject root) {
        roots.computeIfAbsent(rootType, x -> new HashMap<>()).put(rootPath, root);
    }

    public Object getEnclosedField(Class<?> enclosingType, String key, Object enclosingObject) {
        return enclosedThings
                .getOrDefault(enclosingType, Collections.emptyMap())
                .getOrDefault(key, Collections.emptyMap())
                .get(enclosingObject);
    }

    public void registerEnclosedField(Class<?> enclosingType, String key, Object enclosingObject, Object value) {
        enclosedThings
                .computeIfAbsent(enclosingType, x -> new HashMap<>())
                .computeIfAbsent(key, x -> new IdentityHashMap<>())
                .put(enclosingObject, value);
    }

    public <T> T constructGroup(Class<T> interfaceType) {
        Constructor<? extends ConfigurationObject> constructor = getConfigurationInterface(interfaceType).getConstructor();
        ConfigurationObject instance;
        try {
            instance = constructor.newInstance(this);
        } catch (InstantiationException e) {
            throw new InstantiationError(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException | Error e2) {
                throw e2;
            } catch (Throwable t) {
                throw new UndeclaredThrowableException(t);
            }
        }
        allInstances.add(instance);
        return interfaceType.cast(instance);
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getValueConverter(Class<?> enclosingType, String field) {
        return (Converter<T>) convertersByTypeAndField
                .computeIfAbsent(enclosingType, x -> new HashMap<>())
                .computeIfAbsent(field, x -> {
                    ConfigurationInterface ci = getConfigurationInterface(enclosingType);
                    Property property = ci.getProperty(field);
                    boolean optional = property.isOptional();
                    if (property.isLeaf() || optional && property.asOptional().getNestedProperty().isLeaf()) {
                        LeafProperty leafProperty = optional ? property.asOptional().getNestedProperty().asLeaf()
                                : property.asLeaf();
                        if (leafProperty.hasConvertWith()) {
                            Class<? extends Converter<?>> convertWith = leafProperty.getConvertWith();
                            // todo: generics
                            return getConverterInstance(convertWith);
                        } else {
                            // todo: replace with generic converter lookup
                            Class<?> valueRawType = leafProperty.getValueRawType();
                            if (valueRawType == List.class) {
                                return Converters.newCollectionConverter(
                                        config.getConverter(rawTypeOf(typeOfParameter(leafProperty.getValueType(), 0))),
                                        ArrayList::new);
                            } else if (valueRawType == Set.class) {
                                return Converters.newCollectionConverter(
                                        config.getConverter(rawTypeOf(typeOfParameter(leafProperty.getValueType(), 0))),
                                        HashSet::new);
                            } else {
                                return config.getConverter(valueRawType);
                            }
                        }
                    } else if (property.isPrimitive()) {
                        PrimitiveProperty primitiveProperty = property.asPrimitive();
                        if (primitiveProperty.hasConvertWith()) {
                            return getConverterInstance(primitiveProperty.getConvertWith());
                        } else {
                            return config.getConverter(primitiveProperty.getBoxType());
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getKeyConverter(Class<?> enclosingType, String field, int degree) {
        List<Map<Class<?>, Map<String, Converter<?>>>> list = this.keyConvertersByDegreeTypeAndField;
        while (list.size() <= degree) {
            list.add(new IdentityHashMap<>());
        }
        Map<Class<?>, Map<String, Converter<?>>> map = list.get(degree);
        return (Converter<T>) map
                .computeIfAbsent(enclosingType, x -> new HashMap<>())
                .computeIfAbsent(field, x -> {
                    ConfigurationInterface ci = getConfigurationInterface(enclosingType);
                    MapProperty property = ci.getProperty(field).asMap();
                    while (degree + 1 > property.getLevels()) {
                        property = property.getValueProperty().asMap();
                    }
                    if (property.hasKeyConvertWith()) {
                        return getConverterInstance(property.getKeyConvertWith());
                    } else {
                        // todo: replace with generic converter lookup
                        Class<?> valueRawType = property.getKeyRawType();
                        if (valueRawType == List.class) {
                            return Converters.newCollectionConverter(
                                    config.getConverter(rawTypeOf(typeOfParameter(property.getKeyType(), 0))), ArrayList::new);
                        } else if (valueRawType == Set.class) {
                            return Converters.newCollectionConverter(
                                    config.getConverter(rawTypeOf(typeOfParameter(property.getKeyType(), 0))), HashSet::new);
                        } else {
                            return config.getConverter(valueRawType);
                        }
                    }
                });
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<T> getConverterInstance(Class<? extends Converter<? extends T>> converterType) {
        return (Converter<T>) converterInstances.computeIfAbsent(converterType, t -> {
            try {
                return (Converter<T>) t.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new InstantiationError(e.getMessage());
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
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(e.getMessage());
            }
        });
    }

    public NoSuchElementException noSuchElement(Class<?> type) {
        return new NoSuchElementException("A required configuration group of type " + type.getName() + " was not provided");
    }

    public void unknownConfigElement(final ConfigValue configValue) {
    }

    void fillInOptionals() {
        for (ConfigurationObject instance : allInstances) {
            instance.fillInOptionals(this);
        }
    }

    public SmallRyeConfig getConfig() {
        return config;
    }

    public StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    public void reportProblem(RuntimeException problem) {
        problems.add(new Problem(problem.toString()));
    }

    ArrayList<Problem> getProblems() {
        return problems;
    }

    Map<Class<?>, Map<String, ConfigurationObject>> getRootsMap() {
        return roots;
    }
}
