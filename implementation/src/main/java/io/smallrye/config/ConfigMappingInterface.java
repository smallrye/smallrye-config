package io.smallrye.config;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config._private.ConfigMessages;

/**
 * The metadata representation of a {@link ConfigMapping} annotated class.
 */
public final class ConfigMappingInterface implements ConfigMappingMetadata {

    static final ConfigMappingInterface[] NO_TYPES = new ConfigMappingInterface[0];
    static final Property[] NO_PROPERTIES = new Property[0];
    static final ClassValue<ConfigMappingInterface> cv = new ClassValue<>() {
        protected ConfigMappingInterface computeValue(final Class<?> type) {
            return createConfigurationInterface(type);
        }
    };

    private final Class<?> interfaceType;
    private final String className;
    private final ConfigMappingInterface[] superTypes;
    private final Property[] properties;
    private final ToStringMethod toStringMethod;

    ConfigMappingInterface(final Class<?> interfaceType, final ConfigMappingInterface[] superTypes,
            final Property[] properties) {
        this.interfaceType = interfaceType;
        this.className = getImplementationClassName(interfaceType);
        this.superTypes = superTypes;

        List<Property> filteredProperties = new ArrayList<>();
        ToStringMethod toStringMethod = null;
        for (Property property : properties) {
            if (!property.isToStringMethod()) {
                filteredProperties.add(property);
            } else {
                toStringMethod = (ToStringMethod) property;
            }
        }
        // let's make sure the properties are ordered as we might generate code from them
        // and we need code generation to be deterministic
        filteredProperties.sort(PropertyComparator.INSTANCE);
        this.properties = collectFullHierarchyProperties(this, filteredProperties.toArray(Property[]::new));
        this.toStringMethod = toStringMethod != null ? toStringMethod : ToStringMethod.NONE;
    }

    static String getImplementationClassName(Class<?> type) {
        // do not use string concatenation here
        // make sure the impl name doesn't clash with potential user classes
        String className = type.getName();
        StringBuilder implementationClassName = new StringBuilder(className.length() + 8);
        implementationClassName.append(className).append("$$CMImpl");
        return implementationClassName.toString();
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
     * Get the generated implementation class name. The class name is the configuration interface type name, plus the
     * hashcode of the configuration interface type name with the suffix <code>Impl</code>.
     *
     * @return the generated implementation class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Get the array of {@link ConfigMappingInterface} super types relative to this {@link ConfigMappingInterface}. The
     * array includes all super types until <code>java.lang.Object</code>.
     *
     * @return the array of {@link ConfigMappingInterface} super types
     */
    public ConfigMappingInterface[] getSuperTypes() {
        return superTypes;
    }

    /**
     * Get the array of {@link Property} relative to this {@link ConfigMappingInterface}. The array includes all
     * properties, including properties contributed by super types.
     *
     * @return the array of {@link Property}
     */
    public Property[] getProperties() {
        return properties;
    }

    private static Property[] collectFullHierarchyProperties(final ConfigMappingInterface type, final Property[] properties) {
        // We use a Map to override definitions from super members
        // We want the properties to be sorted so that the iteration order is deterministic
        Map<String, Property> fullHierarchyProperties = new TreeMap<>();
        collectSuperProperties(type, fullHierarchyProperties, new HashSet<>());
        for (Property property : properties) {
            fullHierarchyProperties.put(property.getMemberName(), property);
        }
        return fullHierarchyProperties.values().toArray(Property[]::new);
    }

    private static void collectSuperProperties(final ConfigMappingInterface type, Map<String, Property> properties,
            final Set<String> ignored) {
        // to ignore implementation of abstract methods coming from super classes
        for (Method method : type.getInterfaceType().getDeclaredMethods()) {
            if (method.isDefault()) {
                ignored.add(method.getName());
            }
        }

        for (ConfigMappingInterface superType : type.getSuperTypes()) {
            collectSuperProperties(superType, properties, ignored);
            for (Property property : superType.getProperties()) {
                if (!ignored.contains(property.getMemberName())) {
                    properties.put(property.getMemberName(), property);
                }
            }
        }
    }

    ToStringMethod getToStringMethod() {
        return toStringMethod;
    }

    public boolean hasConfigMapping() {
        return interfaceType.getAnnotation(ConfigMapping.class) != null;
    }

    public NamingStrategy getNamingStrategy() {
        ConfigMapping configMapping = interfaceType.getAnnotation(ConfigMapping.class);
        return configMapping != null ? configMapping.namingStrategy() : NamingStrategy.KEBAB_CASE;
    }

    public boolean isBeanStyleGetters() {
        ConfigMapping configMapping = interfaceType.getAnnotation(ConfigMapping.class);
        return configMapping != null && configMapping.beanStyleGetters();
    }

    String getClassInternalName() {
        return className.replace('.', '/');
    }

    List<ConfigMappingInterface> getNested() {
        Set<ConfigMappingInterface> nested = new LinkedHashSet<>();
        getNested(properties, nested);
        return new ArrayList<>(nested);
    }

    public byte[] getClassBytes() {
        try {
            return ConfigMappingGenerator.generate(this);
        } catch (Throwable e) {
            throw ConfigMessages.msg.couldNotGenerateMapping(e, className);
        }
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

        public String getPropertyName(final NamingStrategy namingStrategy) {
            return hasPropertyName() ? getPropertyName() : namingStrategy.apply(getPropertyName());
        }

        public String getMemberName() {
            return method.getName();
        }

        public boolean hasPropertyName() {
            return propertyName != null;
        }

        public boolean hasConvertWith() {
            return false;
        }

        public boolean isParentPropertyName() {
            return hasPropertyName() && propertyName.isEmpty();
        }

        public boolean hasDefaultValue() {
            return false;
        }

        public String getDefaultValue() {
            return null;
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

        public boolean isSecret() {
            return false;
        }

        public boolean isDefaultMethod() {
            return false;
        }

        public boolean isToStringMethod() {
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

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Property property = (Property) o;
            return method.equals(property.method) && propertyName.equals(property.propertyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, propertyName);
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

        @Override
        public LeafProperty asLeaf() {
            return isLeaf() ? nestedProperty.asLeaf() : super.asLeaf();
        }

        @Override
        public boolean isSecret() {
            return asLeaf().isSecret();
        }

        @Override
        public boolean hasDefaultValue() {
            return isLeaf() && nestedProperty.asLeaf().hasDefaultValue();
        }

        @Override
        public String getDefaultValue() {
            return hasDefaultValue() ? nestedProperty.asLeaf().getDefaultValue() : null;
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

        public boolean hasNamingStrategy() {
            return groupType.getInterfaceType().isAnnotationPresent(ConfigMapping.class);
        }

        public NamingStrategy getNamingStrategy() {
            return groupType.getNamingStrategy();
        }
    }

    public static final class LeafProperty extends MayBeOptionalProperty {
        private final Type valueType;
        private final Class<? extends Converter<?>> convertWith;
        private final Class<?> rawType;
        private final String defaultValue;
        private final boolean secret;

        LeafProperty(final Method method, final String propertyName, final Type valueType,
                final Class<? extends Converter<?>> convertWith, final String defaultValue) {
            super(method, propertyName);
            this.valueType = valueType;
            this.convertWith = convertWith;
            this.rawType = rawTypeOf(valueType);
            this.defaultValue = defaultValue;
            this.secret = Secret.class.isAssignableFrom(rawType);
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
            if (secret && valueType instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getActualTypeArguments().length == 1) {
                    return rawTypeOf(parameterizedType.getActualTypeArguments()[0]);
                }
            }
            return rawType;
        }

        public boolean isSecret() {
            return secret;
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
        private final String keyUnnamed;
        private final Class<? extends Supplier<Iterable<String>>> keysProvider;
        private final Class<? extends Converter<?>> keyConvertWith;
        private final Property valueProperty;
        private final boolean hasDefault;
        private final String defaultValue;

        MapProperty(
                final Method method,
                final String propertyName,
                final Type keyType,
                final String keyUnnamed,
                final Class<? extends Supplier<Iterable<String>>> keysProvider,
                final Class<? extends Converter<?>> keyConvertWith,
                final Property valueProperty,
                final boolean hasDefault,
                final String defaultValue) {

            super(method, propertyName);
            this.keyType = keyType;
            this.keyUnnamed = keyUnnamed;
            this.keysProvider = keysProvider;
            this.keyConvertWith = keyConvertWith;
            this.valueProperty = valueProperty;
            this.hasDefault = hasDefault;
            this.defaultValue = defaultValue;
        }

        public Type getKeyType() {
            return keyType;
        }

        public Class<?> getKeyRawType() {
            return rawTypeOf(keyType);
        }

        public String getKeyUnnamed() {
            return keyUnnamed;
        }

        public boolean hasKeyUnnamed() {
            return keyUnnamed != null;
        }

        public Class<? extends Supplier<Iterable<String>>> getKeysProvider() {
            return Assert.checkNotNullParam("keyProvider", keysProvider);
        }

        public boolean hasKeyProvider() {
            return keysProvider != null;
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

        public String getDefaultValue() {
            return defaultValue;
        }

        public boolean hasDefaultValue() {
            return hasDefault;
        }

        @Override
        public boolean isMap() {
            return true;
        }

        @Override
        public MapProperty asMap() {
            return this;
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

    public static final class ToStringMethod extends Property {
        private final boolean generate;

        ToStringMethod() {
            super(null, null);
            this.generate = false;
        }

        ToStringMethod(final Method method) {
            super(method, null);
            this.generate = true;
        }

        @Override
        public boolean isToStringMethod() {
            return true;
        }

        public boolean generate() {
            return generate;
        }

        static final ToStringMethod NONE = new ToStringMethod();
    }

    private static ConfigMappingInterface createConfigurationInterface(Class<?> interfaceType) {
        if (!interfaceType.isInterface() || interfaceType.getTypeParameters().length != 0) {
            return null;
        }
        // No reason to use a JDK interface to generate a config class? Primarily to fix the java.nio.file.Path case.
        if (interfaceType.getName().startsWith("java")) {
            return null;
        }

        if (Secret.class.isAssignableFrom(interfaceType)) {
            return null;
        }

        // first, find any supertypes
        ConfigMappingInterface[] superTypes = getSuperTypes(interfaceType.getInterfaces(), 0, 0);
        // now find any properties
        Property[] properties = getProperties(interfaceType, interfaceType.getDeclaredMethods(), 0, 0);
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

    static Property[] getProperties(Class<?> interfaceType, Method[] methods, int si, int ti) {
        for (int i = si; i < methods.length; i++) {
            Method method = methods[i];
            int mods = method.getModifiers();
            if (!Modifier.isPublic(mods) || Modifier.isStatic(mods) || !Modifier.isAbstract(mods)) {
                // no need for recursive calls here, which are costly in interpreted mode!
                continue;
            }
            if (method.getParameterCount() > 0) {
                throw ConfigMessages.msg.mappingMethodsCannotAcceptParameters(method.getName());
            }
            if (method.getReturnType() == void.class) {
                throw ConfigMessages.msg.mappingMethodsCannotBeVoid(method.getName());
            }
            Property p = getPropertyDef(interfaceType, method, method.getAnnotatedReturnType());
            final Property[] array;
            if (i + 1 == methods.length) {
                array = new Property[ti + 1];
            } else {
                array = getProperties(interfaceType, methods, i + 1, ti + 1);
            }
            array[ti] = p;
            return array;
        }
        return ti > 0 ? new Property[ti] : NO_PROPERTIES;
    }

    private static Property getPropertyDef(Class<?> interfaceType, Method method, AnnotatedType type) {
        if (isToStringMethod(method)) {
            return new ToStringMethod(method);
        }

        String propertyName = getPropertyName(method);

        Method defaultMethod = hasDefaultMethodImplementation(method);
        if (defaultMethod != null) {
            return new DefaultMethodProperty(method, defaultMethod, getPropertyDef(interfaceType, defaultMethod, type));
        }

        // now figure out what kind it is
        Class<? extends Converter<?>> convertWith = getConverter(type, method);
        Class<?> rawType = rawTypeOf(type.getType());
        if (rawType.isPrimitive()) {
            // primitive!
            return new PrimitiveProperty(method, propertyName, rawType, convertWith, getDefaultValue(method));
        }
        if (convertWith == null) {
            if (interfaceType == rawType) {
                throw ConfigMessages.msg.mappingCannotUseSelfReferenceTypes(rawType.getName());
            }

            if (rawType == Optional.class) {
                // optional is special: it can contain a leaf or a group, but not a map (unless it has @ConvertWith)
                Property nested = getPropertyDef(interfaceType, method, typeOfParameter(type, 0));
                if (nested.isMayBeOptional()) {
                    return new OptionalProperty(method, propertyName, nested.asMayBeOptional());
                }
                throw new IllegalArgumentException("Property type " + type + " cannot be optional");
            }
            if (rawType == Map.class) {
                // it's a map...
                AnnotatedType keyType = typeOfParameter(type, 0);
                AnnotatedType valueType = typeOfParameter(type, 1);
                String defaultValue = getDefaultValue(method);
                return new MapProperty(method,
                        propertyName, keyType.getType(),
                        getUnnamedKey(keyType, method),
                        getKeysProvider(keyType, method),
                        getConverter(keyType, method),
                        getPropertyDef(interfaceType, method, valueType),
                        defaultValue != null || hasDefaults(method),
                        defaultValue);
            }
            if (rawType == List.class || rawType == Set.class) {
                AnnotatedType elementType = typeOfParameter(type, 0);
                Class<?> elementClass = rawTypeOf(elementType.getType());
                if (interfaceType == elementClass) {
                    throw ConfigMessages.msg.mappingCannotUseSelfReferenceTypes(elementClass.getName());
                }

                if (elementClass == Map.class) {
                    return new CollectionProperty(rawType, getPropertyDef(interfaceType, method, elementType));
                }

                ConfigMappingInterface configurationInterface = getConfigurationInterface(elementClass);
                if (configurationInterface != null) {
                    return new CollectionProperty(rawType, new GroupProperty(method, propertyName, configurationInterface));
                }

                Class<? extends Converter<?>> converter = getConverter(elementType, method);
                if (converter != null) {
                    convertWith = converter;
                }
                return new CollectionProperty(rawType,
                        new LeafProperty(method, propertyName, elementType.getType(), convertWith, getDefaultValue(method)));
            }
            ConfigMappingInterface configurationInterface = getConfigurationInterface(rawType);
            if (configurationInterface != null) {
                // it's a group
                return new GroupProperty(method, propertyName, configurationInterface);
            }
            // fall out (leaf)
        }

        String defaultValue = getDefaultValue(method);
        if (rawType == List.class || rawType == Set.class) {
            AnnotatedType elementType = typeOfParameter(type, 0);
            Class<? extends Converter<?>> converter = getConverter(elementType, method);
            if (converter != null) {
                convertWith = converter;
            }
            return new CollectionProperty(rawType,
                    new LeafProperty(method, propertyName, elementType.getType(), convertWith, defaultValue));
        } else if (rawType == Optional.class) {
            return new OptionalProperty(method, propertyName,
                    new LeafProperty(method, propertyName, type.getType(), convertWith, defaultValue));
        }

        // otherwise it's a leaf
        return new LeafProperty(method, propertyName, type.getType(), convertWith, defaultValue);
    }

    private static boolean isToStringMethod(Method method) {
        return method.getName().equals("toString") &&
                method.getParameterCount() == 0 &&
                method.getReturnType().equals(String.class) &&
                !method.isDefault();
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

    private static String getDefaultValue(final Method method) {
        WithDefault annotation = method.getAnnotation(WithDefault.class);
        return annotation == null ? null : annotation.value();
    }

    private static boolean hasDefaults(final Method method) {
        return method.getAnnotation(WithDefaults.class) != null;
    }

    private static String getUnnamedKey(final AnnotatedType type, final Method method) {
        WithUnnamedKey annotation = type.getAnnotation(WithUnnamedKey.class);
        if (annotation == null) {
            annotation = method.getAnnotation(WithUnnamedKey.class);
        }
        return annotation != null ? annotation.value() : null;
    }

    private static Class<? extends Supplier<Iterable<String>>> getKeysProvider(final AnnotatedType type, final Method method) {
        WithKeys annotation = type.getAnnotation(WithKeys.class);
        if (annotation == null) {
            annotation = method.getAnnotation(WithKeys.class);
        }
        return annotation != null ? annotation.value() : null;
    }

    private static Class<? extends Converter<?>> getConverter(final AnnotatedType type, final Method method) {
        WithConverter annotation = type.getAnnotation(WithConverter.class);
        // fallback to method
        if (annotation == null) {
            annotation = method.getAnnotation(WithConverter.class);
        }
        if (annotation != null) {
            Class<? extends Converter<?>> value = annotation.value();
            validateConverter(type.getType(), value);
            return value;
        } else {
            return null;
        }
    }

    private static void validateConverter(final Type type, final Class<? extends Converter<?>> convertWith) {
        if (type instanceof Class) {
            try {
                Class<?> classType = (Class<?>) type;
                Class<?> effectiveType = classType.isPrimitive() ? PrimitiveProperty.boxTypes.get(classType) : classType;
                Method convertMethod = convertWith.getMethod("convert", String.class);
                if (!effectiveType.isAssignableFrom(convertMethod.getReturnType())) {
                    throw new IllegalArgumentException();
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
    }

    private static String getPropertyName(final AnnotatedElement element) {
        boolean useParent = element.getAnnotation(WithParentName.class) != null;
        WithName annotation = element.getAnnotation(WithName.class);
        if (annotation != null) {
            if (useParent) {
                throw new IllegalArgumentException("Cannot specify both @WithParentName and @WithName in '" + element + "'");
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

    private static void getNested(final Property[] properties, final Set<ConfigMappingInterface> nested) {
        for (Property property : properties) {
            if (property instanceof GroupProperty groupProperty) {
                ConfigMappingInterface group = groupProperty.getGroupType();
                nested.add(group);
                Collections.addAll(nested, group.superTypes);
                getNested(group.getProperties(), nested);
            }

            if (property instanceof OptionalProperty optionalProperty) {
                if (optionalProperty.getNestedProperty() instanceof GroupProperty groupProperty) {
                    ConfigMappingInterface group = groupProperty.getGroupType();
                    nested.add(group);
                    Collections.addAll(nested, group.superTypes);
                    getNested(group.getProperties(), nested);
                } else if (optionalProperty.getNestedProperty() instanceof CollectionProperty collectionProperty) {
                    getNested(new Property[] { collectionProperty.element }, nested);
                }
            }

            if (property instanceof MapProperty mapProperty) {
                getNested(new Property[] { mapProperty.valueProperty }, nested);
            }

            if (property instanceof CollectionProperty collectionProperty) {
                getNested(new Property[] { collectionProperty.element }, nested);
            }
        }
    }

    static AnnotatedType typeOfParameter(final AnnotatedType type, final int index) {
        if (type instanceof AnnotatedParameterizedType) {
            return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[index];
        } else {
            return type;
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

    /**
     * Constructs a representation of all {@link Property} contained in the {@link ConfigMappingInterface}.
     *
     * <ul>
     * <li>The first level <code>Map</code> key is each <code>Class</code> that is part of the mapping</li>
     * <li>The second level <code>Map</code> key is each <code>String</code> path in the mapping to a {@link Property}</li>
     * <li>The third level <code>Map</code> key is each <code>String</code> {@link Property} name under the mapping
     * path, including sub-elements and nested elements</li>
     * </ul>
     *
     * The path names do not include the {@link ConfigMapping#prefix()} because the same {@link ConfigMappingInterface}
     * can be registered for multiple prefixes.
     * <p>
     * The mapping class root {@link Property} use the empty <code>String</code> for the path key.
     *
     * @param configMapping a {@link ConfigMappingInterface} representation of a {@link ConfigMapping} annotated class
     * @return a <code>Map</code> with {@link Property} values
     */
    public static Map<Class<?>, Map<String, Map<String, Property>>> getProperties(final ConfigMappingInterface configMapping) {
        Map<Class<?>, Map<String, Map<String, Property>>> properties = new HashMap<>();
        getProperties(new GroupProperty(null, null, configMapping), configMapping.getNamingStrategy(), new Path(), properties);
        return properties;
    }

    private static void getProperties(
            final GroupProperty groupProperty,
            final NamingStrategy namingStrategy,
            final Path path,
            final Map<Class<?>, Map<String, Map<String, Property>>> properties) {

        ConfigMappingInterface groupType = groupProperty.getGroupType();
        Map<String, Property> groupProperties = properties
                .computeIfAbsent(groupType.getInterfaceType(), group -> new TreeMap<>())
                .computeIfAbsent(path.get(), s -> new TreeMap<>());

        getProperties(groupProperty, namingStrategy, path, properties, groupProperties);
    }

    private static void getProperties(
            final GroupProperty groupProperty,
            final NamingStrategy namingStrategy,
            final Path path,
            final Map<Class<?>, Map<String, Map<String, Property>>> properties,
            final Map<String, Property> groupProperties) {

        for (Property property : groupProperty.getGroupType().getProperties()) {
            getProperty(property, namingStrategy, path, properties, groupProperties);
        }
    }

    private static void getProperty(
            final Property property,
            final NamingStrategy namingStrategy,
            final Path path,
            final Map<Class<?>, Map<String, Map<String, Property>>> properties,
            final Map<String, Property> groupProperties) {

        if (property.isLeaf() || property.isPrimitive()) {
            groupProperties.put(path.get(property, namingStrategy), property);
        } else if (property.isGroup()) {
            GroupProperty groupProperty = property.asGroup();
            NamingStrategy groupNamingStrategy = groupProperty.hasNamingStrategy() ? groupProperty.getNamingStrategy()
                    : namingStrategy;
            Path groupPath = path.group(groupProperty, namingStrategy);
            getProperties(groupProperty, groupNamingStrategy, groupPath, properties);
            getProperties(groupProperty, groupNamingStrategy, groupPath, properties, groupProperties);
        } else if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            if (mapProperty.getValueProperty().isLeaf()) {
                groupProperties.put(path.map(property, namingStrategy).get(), property);
                if (mapProperty.hasKeyUnnamed()) {
                    groupProperties.put(path.unnamedMap(property, namingStrategy).get(), property);
                }
            } else if (mapProperty.getValueProperty().isGroup()) {
                GroupProperty groupProperty = mapProperty.getValueProperty().asGroup();
                NamingStrategy groupNamingStrategy = groupProperty.hasNamingStrategy() ? groupProperty.getNamingStrategy()
                        : namingStrategy;
                Path mapPath = path.map(mapProperty, namingStrategy);
                getProperties(groupProperty, groupNamingStrategy, mapPath, properties);
                getProperties(groupProperty, groupNamingStrategy, mapPath, properties, groupProperties);
                if (mapProperty.hasKeyUnnamed()) {
                    Path unnamedMapPath = path.unnamedMap(mapProperty, namingStrategy);
                    getProperties(groupProperty, groupNamingStrategy, unnamedMapPath, properties);
                    getProperties(groupProperty, groupNamingStrategy, unnamedMapPath, properties, groupProperties);
                }
            } else if (mapProperty.getValueProperty().isCollection()) {
                CollectionProperty collectionProperty = mapProperty.getValueProperty().asCollection();
                Property element = collectionProperty.getElement();
                if (element.isLeaf()) {
                    groupProperties.put(path.map().collection().get(property, namingStrategy), property);
                    // We use a different leaf to remove the default, to not duplicate defaults properties, because
                    // leaf collections can use the single name or use the name with square brackets
                    groupProperties.put(path.map().get(property, namingStrategy),
                            new LeafProperty(element.getMethod(), element.getPropertyName(), element.asLeaf().getValueType(),
                                    element.asLeaf().getConvertWith(), null));
                    if (mapProperty.hasKeyUnnamed()) {
                        groupProperties.put(path.collection().get(property, namingStrategy), property);
                    }
                } else {
                    getProperty(element, namingStrategy, path.map().collection(), properties, groupProperties);
                    if (mapProperty.hasKeyUnnamed()) {
                        getProperty(element, namingStrategy, path.collection(), properties, groupProperties);
                    }
                }
            } else if (mapProperty.getValueProperty().isMap()) {
                getProperty(mapProperty.getValueProperty(), namingStrategy, path.map(), properties, groupProperties);
                if (mapProperty.hasKeyUnnamed()) {
                    getProperty(mapProperty.getValueProperty(), namingStrategy, path.unnamedMap(), properties, groupProperties);
                }
            }
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            if (collectionProperty.getElement().isLeaf()) {
                getProperty(collectionProperty.getElement(), namingStrategy, path, properties, groupProperties);
            }
            getProperty(collectionProperty.getElement(), namingStrategy, path.collection(), properties, groupProperties);
        } else if (property.isOptional()) {
            getProperty(property.asOptional().getNestedProperty(), namingStrategy, path, properties, groupProperties);
        }
    }

    static class Path {
        private final String path;
        private final List<String> elements;

        Path() {
            this("");
        }

        Path(final String path) {
            this(path, new ArrayList<>());
        }

        Path(final String path, final List<String> elements) {
            this.path = path;
            this.elements = elements;
        }

        Path group(final String path) {
            return new Path(get(path));
        }

        Path group(final Property property, final NamingStrategy namingStrategy) {
            if (property.isParentPropertyName()) {
                return group("");
            } else {
                return group(property.getPropertyName(namingStrategy));
            }
        }

        Path map() {
            List<String> elements = new ArrayList<>(this.elements);
            elements.add(path.isEmpty() && elements.isEmpty() ? "*" : ".*");
            return new Path(get(), elements);
        }

        Path map(final String path) {
            return new Path(get(path));
        }

        Path map(final Property property, final NamingStrategy namingStrategy) {
            if (property.isParentPropertyName()) {
                this.elements.add(path.isEmpty() && this.elements.isEmpty() ? "*" : ".*");
                return map("");
            } else {
                this.elements.add(".*");
                return map(property.getPropertyName(namingStrategy));
            }
        }

        Path collection() {
            List<String> elements = new ArrayList<>(this.elements);
            elements.add("[*]");
            return new Path(get(), elements);
        }

        Path unnamedMap() {
            return this;
        }

        Path unnamedMap(final String path) {
            return new Path(get(path));
        }

        Path unnamedMap(final Property property, final NamingStrategy namingStrategy) {
            return unnamedMap(property.isParentPropertyName() ? "" : property.getPropertyName(namingStrategy));
        }

        String get(final String path) {
            String elements = String.join("", this.elements);
            this.elements.clear();
            if (this.path.isEmpty()) {
                if (path.isEmpty()) {
                    return elements;
                } else {
                    if (!elements.isEmpty() && elements.charAt(0) == '*') {
                        return path + "." + elements;
                    } else {
                        return path + elements;
                    }
                }
            } else {
                return path.isEmpty() ? this.path + elements : this.path + "." + path + elements;
            }
        }

        String get(final Property property, final NamingStrategy namingStrategy) {
            return get(property.isParentPropertyName() ? "" : property.getPropertyName(namingStrategy));
        }

        String get() {
            return path;
        }
    }

    private static class PropertyComparator implements Comparator<Property> {

        private static final PropertyComparator INSTANCE = new PropertyComparator();

        @Override
        public int compare(Property p1, Property p2) {
            return p1.getMemberName().compareTo(p2.getMemberName());
        }
    }
}
