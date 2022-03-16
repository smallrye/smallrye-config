package io.smallrye.config;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.common.utils.StringUtil;

/**
 * Information about a configuration interface.
 */
public final class ConfigMappingInterface implements ConfigMappingMetadata {
    static final ConfigMappingInterface[] NO_TYPES = new ConfigMappingInterface[0];
    static final Property[] NO_PROPERTIES = new Property[0];
    static final ClassValue<ConfigMappingInterface> cv = new ClassValue<ConfigMappingInterface>() {
        protected ConfigMappingInterface computeValue(final Class<?> type) {
            return createConfigurationInterface(type);
        }
    };

    private final Class<?> interfaceType;
    private final String className;
    private final ConfigMappingInterface[] superTypes;
    private final Property[] properties;
    private final Map<String, Property> propertiesByName;
    private final NamingStrategy namingStrategy;

    ConfigMappingInterface(final Class<?> interfaceType, final ConfigMappingInterface[] superTypes,
            final Property[] properties) {
        this.interfaceType = interfaceType;
        this.className = interfaceType.getName() + interfaceType.getName().hashCode() + "Impl";
        this.superTypes = superTypes;
        this.properties = properties;
        this.propertiesByName = toPropertiesMap(properties);
        this.namingStrategy = getNamingStrategy(interfaceType);
    }

    /**
     * Get the configuration interface information for the given interface class. This information is cached.
     *
     * @param interfaceType the interface type (must not be {@code null})
     * @return the configuration interface, or {@code null} if the type does not appear to be a configuration interface
     */
    public static ConfigMappingInterface getConfigurationInterface(Class<?> interfaceType) {
        Assert.checkNotNullParam("interfaceType", interfaceType);
        return cv.get(interfaceType);
    }

    /**
     * Get the configuration interface type.
     *
     * @return the configuration interface type
     */
    public Class<?> getInterfaceType() {
        return interfaceType;
    }

    /**
     * Get the number of supertypes which define configuration properties. Implemented interfaces which do not
     * define any configuration properties and whose supertypes in turn do not define any configuration properties
     * are not counted.
     *
     * @return the number of supertypes
     */
    int getSuperTypeCount() {
        return superTypes.length;
    }

    ConfigMappingInterface[] getSuperTypes() {
        return superTypes;
    }

    /**
     * Get the supertype at the given index, which must be greater than or equal to zero and less than the value returned
     * by {@link #getSuperTypeCount()}.
     *
     * @param index the index
     * @return the supertype definition
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    ConfigMappingInterface getSuperType(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= superTypes.length)
            throw new IndexOutOfBoundsException();
        return superTypes[index];
    }

    public Property[] getProperties() {
        return properties;
    }

    /**
     * Get the number of properties defined on this type (excluding supertypes).
     *
     * @return the number of properties
     */
    int getPropertyCount() {
        return properties.length;
    }

    /**
     * Get the property definition at the given index, which must be greater than or equal to zero and less than the
     * value returned by {@link #getPropertyCount()}.
     *
     * @param index the index
     * @return the property definition
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    Property getProperty(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= properties.length)
            throw new IndexOutOfBoundsException();
        return properties[index];
    }

    Property getProperty(final String name) {
        return propertiesByName.get(name);
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public String getClassName() {
        return className;
    }

    String getClassInternalName() {
        return className.replace('.', '/');
    }

    List<ConfigMappingInterface> getNested() {
        ArrayList<ConfigMappingInterface> nested = new ArrayList<>();
        getNested(properties, nested);
        return nested;
    }

    public byte[] getClassBytes() {
        return ConfigMappingGenerator.generate(this);
    }

    public static abstract class Property {
        private final Method method;
        private final String propertyName;

        Property(final Method method, final String propertyName) {
            this.method = method;
            this.propertyName = propertyName;
        }

        public Method getMethod() {
            return method;
        }

        public String getPropertyName() {
            if (isParentPropertyName()) {
                return propertyName;
            }
            return hasPropertyName() && !propertyName.isEmpty() ? propertyName : method.getName();
        }

        public boolean hasPropertyName() {
            return propertyName != null;
        }

        public boolean isParentPropertyName() {
            return hasPropertyName() && propertyName.isEmpty();
        }

        public boolean isPrimitive() {
            return false;
        }

        public boolean isOptional() {
            return false;
        }

        public boolean isGroup() {
            return false;
        }

        public boolean isLeaf() {
            return false;
        }

        public boolean isMap() {
            return false;
        }

        public boolean isMayBeOptional() {
            return false;
        }

        public boolean isCollection() {
            return false;
        }

        public boolean isDefaultMethod() {
            return false;
        }

        public PrimitiveProperty asPrimitive() {
            throw new ClassCastException();
        }

        public OptionalProperty asOptional() {
            throw new ClassCastException();
        }

        public GroupProperty asGroup() {
            throw new ClassCastException();
        }

        public LeafProperty asLeaf() {
            throw new ClassCastException();
        }

        public MapProperty asMap() {
            throw new ClassCastException();
        }

        public MayBeOptionalProperty asMayBeOptional() {
            throw new ClassCastException();
        }

        public CollectionProperty asCollection() {
            throw new ClassCastException();
        }

        public DefaultMethodProperty asDefaultMethod() {
            throw new ClassCastException();
        }
    }

    public static abstract class MayBeOptionalProperty extends Property {
        MayBeOptionalProperty(final Method method, final String propertyName) {
            super(method, propertyName);
        }

        @Override
        public boolean isMayBeOptional() {
            return true;
        }

        @Override
        public MayBeOptionalProperty asMayBeOptional() {
            return this;
        }
    }

    public static final class PrimitiveProperty extends Property {
        private static final Map<Class<?>, Class<?>> boxTypes;
        private static final Map<Class<?>, String> unboxMethodName;
        private static final Map<Class<?>, String> unboxMethodDesc;

        static {
            Map<Class<?>, Class<?>> map = new HashMap<>();
            map.put(byte.class, Byte.class);
            map.put(short.class, Short.class);
            map.put(int.class, Integer.class);
            map.put(long.class, Long.class);

            map.put(float.class, Float.class);
            map.put(double.class, Double.class);

            map.put(boolean.class, Boolean.class);

            map.put(char.class, Character.class);
            boxTypes = map;
            Map<Class<?>, String> nameMap = new HashMap<>();
            nameMap.put(byte.class, "byteValue");
            nameMap.put(short.class, "shortValue");
            nameMap.put(int.class, "intValue");
            nameMap.put(long.class, "longValue");

            nameMap.put(float.class, "floatValue");
            nameMap.put(double.class, "doubleValue");

            nameMap.put(boolean.class, "booleanValue");

            nameMap.put(char.class, "charValue");
            unboxMethodName = nameMap;
            nameMap = new HashMap<>();
            nameMap.put(byte.class, "()B");
            nameMap.put(short.class, "()S");
            nameMap.put(int.class, "()I");
            nameMap.put(long.class, "()J");

            nameMap.put(float.class, "()F");
            nameMap.put(double.class, "()D");

            nameMap.put(boolean.class, "()Z");

            nameMap.put(char.class, "()C");
            unboxMethodDesc = nameMap;
            nameMap = new HashMap<>();
            nameMap.put(byte.class, "B");
            nameMap.put(short.class, "S");
            nameMap.put(int.class, "I");
            nameMap.put(long.class, "J");

            nameMap.put(float.class, "F");
            nameMap.put(double.class, "D");

            nameMap.put(boolean.class, "Z");

            nameMap.put(char.class, "C");
        }

        private final Class<?> primitiveType;
        private final Class<? extends Converter<?>> convertWith;
        private final String defaultValue;

        PrimitiveProperty(final Method method, final String propertyName, final Class<?> primitiveType,
                final Class<? extends Converter<?>> convertWith, final String defaultValue) {
            super(method, propertyName);
            this.primitiveType = primitiveType;
            this.convertWith = convertWith;
            this.defaultValue = defaultValue;
        }

        public Class<?> getPrimitiveType() {
            return primitiveType;
        }

        public Class<?> getBoxType() {
            return boxTypes.get(primitiveType);
        }

        public Class<? extends Converter<?>> getConvertWith() {
            return Assert.checkNotNullParam("convertWith", convertWith);
        }

        public boolean hasConvertWith() {
            return convertWith != null;
        }

        public String getDefaultValue() {
            return Assert.checkNotNullParam("defaultValue", defaultValue);
        }

        public boolean hasDefaultValue() {
            return defaultValue != null;
        }

        @Override
        public boolean isPrimitive() {
            return true;
        }

        @Override
        public PrimitiveProperty asPrimitive() {
            return this;
        }

        String getUnboxMethodName() {
            return unboxMethodName.get(primitiveType);
        }

        String getUnboxMethodDescriptor() {
            return unboxMethodDesc.get(primitiveType);
        }
    }

    public static final class OptionalProperty extends Property {
        private final MayBeOptionalProperty nestedProperty;

        OptionalProperty(final Method method, final String propertyName, final MayBeOptionalProperty nestedProperty) {
            super(method, propertyName);
            this.nestedProperty = nestedProperty;
        }

        @Override
        public boolean isOptional() {
            return true;
        }

        @Override
        public OptionalProperty asOptional() {
            return this;
        }

        @Override
        public boolean isLeaf() {
            return nestedProperty.isLeaf();
        }

        public MayBeOptionalProperty getNestedProperty() {
            return nestedProperty;
        }
    }

    public static final class GroupProperty extends MayBeOptionalProperty {
        private final ConfigMappingInterface groupType;

        GroupProperty(final Method method, final String propertyName, final ConfigMappingInterface groupType) {
            super(method, propertyName);
            this.groupType = groupType;
        }

        public ConfigMappingInterface getGroupType() {
            return groupType;
        }

        @Override
        public boolean isGroup() {
            return true;
        }

        @Override
        public GroupProperty asGroup() {
            return this;
        }
    }

    public static final class LeafProperty extends MayBeOptionalProperty {
        private final Type valueType;
        private final Class<? extends Converter<?>> convertWith;
        private final Class<?> rawType;
        private final String defaultValue;

        LeafProperty(final Method method, final String propertyName, final Type valueType,
                final Class<? extends Converter<?>> convertWith, final String defaultValue) {
            super(method, propertyName);
            this.valueType = valueType;
            this.convertWith = convertWith;
            rawType = rawTypeOf(valueType);
            this.defaultValue = defaultValue;
        }

        public Type getValueType() {
            return valueType;
        }

        public Class<? extends Converter<?>> getConvertWith() {
            return convertWith;
        }

        public boolean hasConvertWith() {
            return convertWith != null;
        }

        public String getDefaultValue() {
            return Assert.checkNotNullParam("defaultValue", defaultValue);
        }

        public boolean hasDefaultValue() {
            return defaultValue != null;
        }

        public Class<?> getValueRawType() {
            return rawType;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public LeafProperty asLeaf() {
            return this;
        }
    }

    public static final class MapProperty extends Property {
        private final Type keyType;
        private final Class<? extends Converter<?>> keyConvertWith;
        private final Property valueProperty;

        MapProperty(final Method method, final String propertyName, final Type keyType,
                final Class<? extends Converter<?>> keyConvertWith, final Property valueProperty) {
            super(method, propertyName);
            this.keyType = keyType;
            this.keyConvertWith = keyConvertWith;
            this.valueProperty = valueProperty;
        }

        public Type getKeyType() {
            return keyType;
        }

        public Class<?> getKeyRawType() {
            return rawTypeOf(keyType);
        }

        public Class<? extends Converter<?>> getKeyConvertWith() {
            return Assert.checkNotNullParam("keyConvertWith", keyConvertWith);
        }

        public boolean hasKeyConvertWith() {
            return keyConvertWith != null;
        }

        public Property getValueProperty() {
            return valueProperty;
        }

        @Override
        public boolean isMap() {
            return true;
        }

        @Override
        public MapProperty asMap() {
            return this;
        }

        public int getLevels() {
            if (valueProperty.isMap()) {
                return valueProperty.asMap().getLevels() + 1;
            } else {
                return 1;
            }
        }
    }

    public static final class CollectionProperty extends MayBeOptionalProperty {
        private final Class<?> collectionRawType;
        private final Property element;

        CollectionProperty(final Class<?> collectionType, final Property element) {
            super(element.getMethod(), element.hasPropertyName() ? element.getPropertyName() : null);
            this.collectionRawType = collectionType;
            this.element = element;
        }

        public Class<?> getCollectionRawType() {
            return collectionRawType;
        }

        public Property getElement() {
            return element;
        }

        @Override
        public boolean isCollection() {
            return true;
        }

        @Override
        public CollectionProperty asCollection() {
            return this;
        }
    }

    public static final class DefaultMethodProperty extends Property {
        private final Method defaultMethod;
        private final Property defaultProperty;

        DefaultMethodProperty(
                final Method method,
                final Method defaultMethod,
                final Property defaultProperty) {
            super(method, "");
            this.defaultMethod = defaultMethod;
            this.defaultProperty = defaultProperty;
        }

        public Method getDefaultMethod() {
            return defaultMethod;
        }

        public Property getDefaultProperty() {
            return defaultProperty;
        }

        @Override
        public boolean isDefaultMethod() {
            return true;
        }

        @Override
        public DefaultMethodProperty asDefaultMethod() {
            return this;
        }
    }

    private static ConfigMappingInterface createConfigurationInterface(Class<?> interfaceType) {
        if (!interfaceType.isInterface() || interfaceType.getTypeParameters().length != 0) {
            return null;
        }
        // No reason to use a JDK interface to generate a config class? Primarily to fix the java.nio.file.Path case.
        if (interfaceType.getName().startsWith("java")) {
            return null;
        }

        // first, find any supertypes
        ConfigMappingInterface[] superTypes = getSuperTypes(interfaceType.getInterfaces(), 0, 0);
        // now find any properties
        Property[] properties = getProperties(interfaceType.getDeclaredMethods(), 0, 0);
        // is it anything?
        return new ConfigMappingInterface(interfaceType, superTypes, properties);
    }

    private static ConfigMappingInterface[] getSuperTypes(Class<?>[] interfaces, int si, int ti) {
        if (si == interfaces.length) {
            if (ti == 0) {
                return NO_TYPES;
            } else {
                return new ConfigMappingInterface[ti];
            }
        }
        Class<?> item = interfaces[si];
        ConfigMappingInterface ci = getConfigurationInterface(item);
        if (ci != null) {
            ConfigMappingInterface[] array = getSuperTypes(interfaces, si + 1, ti + 1);
            array[ti] = ci;
            return array;
        } else {
            return getSuperTypes(interfaces, si + 1, ti);
        }
    }

    static Property[] getProperties(Method[] methods, int si, int ti) {
        if (si == methods.length) {
            if (ti == 0) {
                return NO_PROPERTIES;
            } else {
                return new Property[ti];
            }
        }
        Method method = methods[si];
        int mods = method.getModifiers();
        if (!Modifier.isPublic(mods) || Modifier.isStatic(mods) || !Modifier.isAbstract(mods)) {
            return getProperties(methods, si + 1, ti);
        }
        if (method.getParameterCount() > 0) {
            throw new IllegalArgumentException("Configuration methods cannot accept parameters");
        }
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("Void config methods are not allowed");
        }
        Property p = getPropertyDef(method, method.getGenericReturnType());
        Property[] array = getProperties(methods, si + 1, ti + 1);
        array[ti] = p;
        return array;
    }

    private static Property getPropertyDef(Method method, Type type) {
        Method defaultMethod = hasDefaultMethodImplementation(method);
        if (defaultMethod != null) {
            return new DefaultMethodProperty(method, defaultMethod, getPropertyDef(defaultMethod, type));
        }

        // now figure out what kind it is
        Class<? extends Converter<?>> convertWith = getConvertWith(type);
        if (convertWith == null) {
            WithConverter withConverter = method.getAnnotation(WithConverter.class);
            if (withConverter != null) {
                convertWith = withConverter.value();
            }
        }
        String propertyName = getPropertyName(method);
        Class<?> rawType = rawTypeOf(type);
        if (rawType.isPrimitive()) {
            // primitive!
            WithDefault annotation = method.getAnnotation(WithDefault.class);
            return new PrimitiveProperty(method, propertyName, rawType, convertWith,
                    annotation == null ? null : annotation.value());
        }
        if (convertWith == null) {
            if (rawType == Optional.class) {
                // optional is special: it can contain a leaf or a group, but not a map (unless it has @ConvertWith)
                Property nested = getPropertyDef(method, typeOfParameter(type, 0));
                if (nested.isMayBeOptional()) {
                    return new OptionalProperty(method, propertyName, nested.asMayBeOptional());
                }
                throw new IllegalArgumentException("Property type " + type + " cannot be optional");
            }
            if (rawType == Map.class) {
                // it's a map...
                Type keyType = typeOfParameter(type, 0);
                Class<? extends Converter<?>> keyConvertWith = getConvertWith(keyType);
                Type valueType = typeOfParameter(type, 1);
                return new MapProperty(method, propertyName, keyType, keyConvertWith, getPropertyDef(method, valueType));
            }
            if (rawType == List.class || rawType == Set.class) {
                Type elementType = typeOfParameter(type, 0);

                if (rawTypeOf(elementType) == Map.class) {
                    return new CollectionProperty(rawType, getPropertyDef(method, elementType));
                }

                ConfigMappingInterface configurationInterface = getConfigurationInterface((Class<?>) elementType);
                if (configurationInterface != null) {
                    return new CollectionProperty(rawType, new GroupProperty(method, propertyName, configurationInterface));
                }

                WithDefault annotation = method.getAnnotation(WithDefault.class);
                return new CollectionProperty(rawType, new LeafProperty(method, propertyName, elementType, null,
                        annotation == null ? null : annotation.value()));
            }
            ConfigMappingInterface configurationInterface = getConfigurationInterface(rawType);
            if (configurationInterface != null) {
                // it's a group
                return new GroupProperty(method, propertyName, configurationInterface);
            }
            // fall out (leaf)
        }

        if (rawType == List.class || rawType == Set.class) {
            Type elementType = typeOfParameter(type, 0);
            WithDefault annotation = method.getAnnotation(WithDefault.class);
            return new CollectionProperty(rawType,
                    new LeafProperty(method, propertyName, elementType, convertWith,
                            annotation == null ? null : annotation.value()));
        }

        // otherwise it's a leaf
        WithDefault annotation = method.getAnnotation(WithDefault.class);
        return new LeafProperty(method, propertyName, type, convertWith, annotation == null ? null : annotation.value());
    }

    @SuppressWarnings("squid:S1872")
    private static Method hasDefaultMethodImplementation(Method method) {
        Class<?> methodClass = method.getDeclaringClass();
        Class<?>[] memberClasses = methodClass.getClasses();
        for (Class<?> memberClass : memberClasses) {
            if (memberClass.getSimpleName().equals("DefaultImpls")) {
                Method candidateMethod;
                try {
                    candidateMethod = memberClass.getMethod(method.getName(), methodClass);
                } catch (NoSuchMethodException e) {
                    return null;
                }

                if (candidateMethod.getReturnType().equals(method.getReturnType())) {
                    return candidateMethod;
                }
            }
        }
        return null;
    }

    private static Class<? extends Converter<?>> getConvertWith(final Type type) {
        if (type instanceof AnnotatedType) {
            WithConverter annotation = ((AnnotatedType) type).getAnnotation(WithConverter.class);
            if (annotation != null) {
                return annotation.value();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static String getPropertyName(final AnnotatedElement element) {
        boolean useParent = element.getAnnotation(WithParentName.class) != null;
        WithName annotation = element.getAnnotation(WithName.class);
        if (annotation != null) {
            if (useParent) {
                throw new IllegalArgumentException("Cannot specify both @ParentConfigName and @ConfigName");
            }
            String name = annotation.value();
            if (!name.isEmpty()) {
                // already interned, effectively
                return name;
            }
            // else invalid name
            throw new IllegalArgumentException("Property name is empty");
        } else if (useParent) {
            return "";
        } else {
            return null;
        }
    }

    private static Map<String, Property> toPropertiesMap(final Property[] properties) {
        Map<String, Property> map = new HashMap<>();
        for (Property p : properties) {
            map.put(p.getMethod().getName(), p);
        }
        return map;
    }

    private static void getNested(final Property[] properties, final List<ConfigMappingInterface> nested) {
        for (Property property : properties) {
            if (property instanceof GroupProperty) {
                GroupProperty groupProperty = (GroupProperty) property;
                ConfigMappingInterface group = groupProperty.getGroupType();
                nested.add(group);
                getNested(group.properties, nested);
            }

            if (property instanceof OptionalProperty) {
                OptionalProperty optionalProperty = (OptionalProperty) property;
                if (optionalProperty.getNestedProperty() instanceof GroupProperty) {
                    GroupProperty groupProperty = (GroupProperty) optionalProperty.getNestedProperty();
                    ConfigMappingInterface group = groupProperty.getGroupType();
                    nested.add(group);
                    getNested(group.properties, nested);
                } else if (optionalProperty.getNestedProperty() instanceof CollectionProperty) {
                    CollectionProperty collectionProperty = (CollectionProperty) optionalProperty.getNestedProperty();
                    getNested(new Property[] { collectionProperty.element }, nested);
                }
            }

            if (property instanceof MapProperty) {
                MapProperty mapProperty = (MapProperty) property;
                getNested(new Property[] { mapProperty.valueProperty }, nested);
            }

            if (property instanceof CollectionProperty) {
                CollectionProperty collectionProperty = (CollectionProperty) property;
                getNested(new Property[] { collectionProperty.element }, nested);
            }
        }
    }

    static Type typeOfParameter(final Type type, final int index) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments()[index];
        } else {
            return null;
        }
    }

    static Class<?> rawTypeOf(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return rawTypeOf(((ParameterizedType) type).getRawType());
        } else if (type instanceof GenericArrayType) {
            return Array.newInstance(rawTypeOf(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        } else if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds != null) {
                return rawTypeOf(upperBounds[0]);
            } else {
                return Object.class;
            }
        } else {
            throw ConfigMessages.msg.noRawType(type);
        }
    }

    private static NamingStrategy getNamingStrategy(final Class<?> interfaceType) {
        final ConfigMapping configMapping = interfaceType.getAnnotation(ConfigMapping.class);
        if (configMapping != null) {
            switch (configMapping.namingStrategy()) {
                case VERBATIM:
                    return VERBATIM_NAMING_STRATEGY;
                case KEBAB_CASE:
                    return KEBAB_CASE_NAMING_STRATEGY;
                case SNAKE_CASE:
                    return SNAKE_CASE_NAMING_STRATEGY;
            }
        }

        return DEFAULT_NAMING_STRATEGY;
    }

    private static final NamingStrategy DEFAULT_NAMING_STRATEGY = new KebabNamingStrategy();
    private static final NamingStrategy VERBATIM_NAMING_STRATEGY = new VerbatimNamingStrategy();
    private static final NamingStrategy KEBAB_CASE_NAMING_STRATEGY = new KebabNamingStrategy();
    private static final NamingStrategy SNAKE_CASE_NAMING_STRATEGY = new SnakeNamingStrategy();

    public interface NamingStrategy extends Function<String, String> {
        default boolean isDefault() {
            return this.equals(DEFAULT_NAMING_STRATEGY);
        }
    }

    static class VerbatimNamingStrategy implements NamingStrategy {
        @Override
        public String apply(final String s) {
            return s;
        }
    }

    static class KebabNamingStrategy implements NamingStrategy {
        @Override
        public String apply(final String s) {
            return StringUtil.skewer(s, '-');
        }
    }

    static class SnakeNamingStrategy implements NamingStrategy {
        @Override
        public String apply(final String s) {
            return StringUtil.skewer(s, '_');
        }
    }
}
