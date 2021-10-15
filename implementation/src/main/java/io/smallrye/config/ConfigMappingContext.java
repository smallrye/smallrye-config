package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterface.LeafProperty;
import static io.smallrye.config.ConfigMappingInterface.MapProperty;
import static io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import static io.smallrye.config.ConfigMappingInterface.Property;
import static io.smallrye.config.ConfigMappingInterface.getConfigurationInterface;
import static io.smallrye.config.ConfigMappingInterface.rawTypeOf;
import static io.smallrye.config.ConfigMappingInterface.typeOfParameter;
import static io.smallrye.config.ConfigValidationException.Problem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.IntFunction;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.NamingStrategy;

/**
 * A mapping context. This is used by generated classes during configuration mapping, and is released once the configuration
 * mapping has completed.
 */
public final class ConfigMappingContext {
    private final Map<Class<?>, Map<String, Map<Object, Object>>> enclosedThings = new IdentityHashMap<>();
    private final Map<Class<?>, Map<String, ConfigMappingObject>> roots = new IdentityHashMap<>();
    private final Map<Class<?>, Map<String, Converter<?>>> convertersByTypeAndField = new IdentityHashMap<>();
    private final List<Map<Class<?>, Map<String, Converter<?>>>> keyConvertersByDegreeTypeAndField = new ArrayList<>();
    private final Map<Class<?>, Converter<?>> converterInstances = new IdentityHashMap<>();
    private final List<ConfigMappingObject> allInstances = new ArrayList<>();
    private final SmallRyeConfig config;
    private final StringBuilder stringBuilder = new StringBuilder();
    private final ArrayList<Problem> problems = new ArrayList<>();

    private NamingStrategy namingStrategy = null;

    ConfigMappingContext(final SmallRyeConfig config) {
        this.config = config;
    }

    public ConfigMappingObject getRoot(Class<?> rootType, String rootPath) {
        return roots.getOrDefault(rootType, Collections.emptyMap()).get(rootPath);
    }

    public void registerRoot(Class<?> rootType, String rootPath, ConfigMappingObject root) {
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

    public <T> T constructRoot(Class<T> interfaceType) {
        this.namingStrategy = ConfigMappingInterface.getConfigurationInterface(interfaceType).getNamingStrategy();
        return constructGroup(interfaceType);
    }

    public <T> T constructGroup(Class<T> interfaceType) {
        final T mappingObject = ConfigMappingLoader.configMappingObject(interfaceType, this);
        allInstances.add((ConfigMappingObject) mappingObject);
        return mappingObject;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    public <T> Converter<T> getValueConverter(Class<?> enclosingType, String field) {
        return (Converter<T>) convertersByTypeAndField
                .computeIfAbsent(enclosingType, x -> new HashMap<>())
                .computeIfAbsent(field, x -> {
                    ConfigMappingInterface ci = getConfigurationInterface(enclosingType);
                    Property property = ci.getProperty(field);
                    return getConverter(property);
                });
    }

    private Converter<?> getConverter(final Property property) {
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
                    return config.requireConverter(rawTypeOf(typeOfParameter(leafProperty.getValueType(), 0)));
                } else if (valueRawType == Set.class) {
                    return config.requireConverter(rawTypeOf(typeOfParameter(leafProperty.getValueType(), 0)));
                } else {
                    return config.requireConverter(valueRawType);
                }
            }
        } else if (property.isPrimitive()) {
            PrimitiveProperty primitiveProperty = property.asPrimitive();
            if (primitiveProperty.hasConvertWith()) {
                return getConverterInstance(primitiveProperty.getConvertWith());
            } else {
                return config.requireConverter(primitiveProperty.getBoxType());
            }
        } else if (property.isCollection() || optional && property.asOptional().getNestedProperty().isCollection()) {
            CollectionProperty collectionProperty = optional ? property.asOptional().getNestedProperty().asCollection()
                    : property.asCollection();
            return getConverter(collectionProperty.getElement());
        } else {
            throw new IllegalStateException();
        }
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
                    ConfigMappingInterface ci = getConfigurationInterface(enclosingType);
                    Property property = ci.getProperty(field);
                    MapProperty mapProperty;
                    if (property.isMap()) {
                        mapProperty = property.asMap();
                    } else if (property.isCollection()) {
                        mapProperty = property.asCollection().getElement().asMap();
                    } else {
                        throw new IllegalStateException();
                    }

                    while (degree + 1 > mapProperty.getLevels()) {
                        mapProperty = mapProperty.getValueProperty().asMap();
                    }
                    if (mapProperty.hasKeyConvertWith()) {
                        return getConverterInstance(mapProperty.getKeyConvertWith());
                    } else {
                        // todo: replace with generic converter lookup
                        Class<?> valueRawType = mapProperty.getKeyRawType();
                        if (valueRawType == List.class) {
                            return Converters.newCollectionConverter(
                                    config.requireConverter(rawTypeOf(typeOfParameter(mapProperty.getKeyType(), 0))),
                                    ArrayList::new);
                        } else if (valueRawType == Set.class) {
                            return Converters.newCollectionConverter(
                                    config.requireConverter(rawTypeOf(typeOfParameter(mapProperty.getKeyType(), 0))),
                                    HashSet::new);
                        } else {
                            return config.requireConverter(valueRawType);
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

    void applyNamingStrategy(final NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public String applyNamingStrategy(final String name) {
        return namingStrategy.apply(name);
    }

    public static IntFunction<Collection<?>> createCollectionFactory(final Class<?> type) {
        if (type == List.class) {
            return ArrayList::new;
        }

        if (type == Set.class) {
            return HashSet::new;
        }

        throw new IllegalArgumentException();
    }

    public NoSuchElementException noSuchElement(Class<?> type) {
        return new NoSuchElementException("A required configuration group of type " + type.getName() + " was not provided");
    }

    public void unknownConfigElement(final String propertyName) {
        problems.add(new Problem(propertyName + " does not map to any root"));
    }

    void fillInOptionals() {
        for (ConfigMappingObject instance : allInstances) {
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

    Map<Class<?>, Map<String, ConfigMappingObject>> getRootsMap() {
        return roots;
    }
}
