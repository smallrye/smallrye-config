package io.smallrye.config;

import static io.smallrye.config.ConfigValidationException.Problem;
import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;

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

import io.smallrye.config.ConfigMappingInterface.NamingStrategy;
import io.smallrye.config._private.ConfigMessages;
import io.smallrye.config.common.utils.StringUtil;

/**
 * A mapping context. This is used by generated classes during configuration mapping, and is released once the configuration
 * mapping has completed.
 */
public final class ConfigMappingContext {
    private final Map<Class<?>, Map<String, Map<Object, Object>>> enclosedThings = new IdentityHashMap<>();
    private final Map<Class<?>, Map<String, ConfigMappingObject>> roots = new IdentityHashMap<>();
    private final Map<Class<?>, Converter<?>> converterInstances = new IdentityHashMap<>();
    private final List<ConfigMappingObject> allInstances = new ArrayList<>();
    private final SmallRyeConfig config;
    private final StringBuilder stringBuilder = new StringBuilder();
    private final Set<String> unknownProperties = new HashSet<>();
    private final List<Problem> problems = new ArrayList<>();
    private NamingStrategy namingStrategy = new ConfigMappingInterface.KebabNamingStrategy();

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
        if (!namingStrategy.isDefault()) {
            this.namingStrategy = namingStrategy;
        }
    }

    public String applyNamingStrategy(final String name) {
        return namingStrategy.apply(name);
    }

    public static <K, V> Map<K, V> createMapWithDefault(final V defaultValue) {
        return new MapWithDefault<>(defaultValue);
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

    void unknownProperty(final String unknownProperty) {
        unknownProperties.add(unknownProperty);
    }

    void reportUnknown() {
        // an unknown property may still be used if it was coming from the EnvSource
        for (String unknownProperty : unknownProperties) {
            boolean found = false;
            String unknownEnvProperty = replaceNonAlphanumericByUnderscores(unknownProperty);
            for (String userProperty : config.getPropertyNames()) {
                if (unknownProperty.equals(userProperty)) {
                    continue;
                }

                // Match another property with the same semantic meaning
                if (StringUtil.equalsIgnoreCaseReplacingNonAlphanumericByUnderscores(unknownEnvProperty, userProperty)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                ConfigValue configValue = config.getConfigValue(unknownProperty);
                problems.add(new Problem(
                        ConfigMessages.msg.propertyDoesNotMapToAnyRoot(unknownProperty, configValue.getLocation())));
            }
        }
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

    List<Problem> getProblems() {
        return problems;
    }

    Map<Class<?>, Map<String, ConfigMappingObject>> getRootsMap() {
        return roots;
    }

    static class MapWithDefault<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1390928078837140814L;
        private final V defaultValue;

        MapWithDefault(final V defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public V get(final Object key) {
            return getOrDefault(key, defaultValue);
        }
    }
}
