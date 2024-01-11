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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.constraint.Assert;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config._private.ConfigMessages;

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
    private final ToStringMethod toStringMethod;

    ConfigMappingInterface(final Class<?> interfaceType, final ConfigMappingInterface[] superTypes,
            final Property[] properties) {
        this.interfaceType = interfaceType;
        this.className = interfaceType.getName() + interfaceType.getName().hashCode() + "Impl";
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
        if (toStringMethod == null) {
            toStringMethod = ToStringMethod.NONE;
        }

        this.properties = filteredProperties.toArray(new Property[0]);
        this.toStringMethod = toStringMethod;
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

    public ConfigMappingInterface[] getSuperTypes() {
        return superTypes;
    }

    public Property[] getProperties() {
        return properties;
    }

    public Property[] getProperties(boolean includeSuper) {
        if (includeSuper) {
            Map<String, Property> properties = getSuperProperties(this);
            for (Property property : this.properties) {
                properties.put(property.getMemberName(), property);
            }
            return properties.values().toArray(new Property[0]);
        } else {
            return getProperties();
        }
    }

    private static Map<String, Property> getSuperProperties(ConfigMappingInterface type) {
        Map<String, Property> properties = new HashMap<>();
        for (ConfigMappingInterface superType : type.getSuperTypes()) {
            properties.putAll(getSuperProperties(superType));
            for (Property property : superType.getProperties()) {
                properties.put(property.getMemberName(), property);
            }
        }
        return properties;
    }

    public boolean hasNamingStrategy() {
        return interfaceType.getAnnotation(ConfigMapping.class) != null;
    }

    public NamingStrategy getNamingStrategy() {
        ConfigMapping configMapping = interfaceType.getAnnotation(ConfigMapping.class);
        return configMapping != null ? configMapping.namingStrategy() : NamingStrategy.KEBAB_CASE;
    }

    ToStringMethod getToStringMethod() {
        return toStringMethod;
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
            boolean result = method.equals(property.method) && propertyName.equals(property.propertyName);
            if (result) {
                return result;
            }
            return isMethodInHierarchy(property.getMethod().getDeclaringClass(), method);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method, propertyName);
        }

        private static boolean isMethodInHierarchy(final Class<?> declaringClass, final Method method) {
            for (Class<?> parent : declaringClass.getInterfaces()) {
                for (final Method parentMethod : parent.getMethods()) {
                    if (parentMethod.getName().equals(method.getName())) {
                        return true;
                    }
                }
                boolean methodInHierarchy = isMethodInHierarchy(parent, method);
                if (methodInHierarchy) {
                    return true;
                }
            }
            return false;
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

        LeafProperty(final Method method, final String propertyName, final Type valueType,
                final Class<? extends Converter<?>> convertWith, final String defaultValue) {
            super(method, propertyName);
            this.valueType = valueType;
            this.convertWith = convertWith;
            this.rawType = rawTypeOf(valueType);
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
        private final String keyUnnamed;
        private final Class<? extends Converter<?>> keyConvertWith;
        private final Property valueProperty;
        private final boolean hasDefault;
        private final String defaultValue;

        MapProperty(
                final Method method,
                final String propertyName,
                final Type keyType,
                final String keyUnnamed,
                final Class<? extends Converter<?>> keyConvertWith,
                final Property valueProperty,
                final boolean hasDefault,
                final String defaultValue) {

            super(method, propertyName);
            this.keyType = keyType;
            this.keyUnnamed = keyUnnamed;
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
        for (int i = si; i < methods.length; i++) {
            Method method = methods[i];
            int mods = method.getModifiers();
            if (!Modifier.isPublic(mods) || Modifier.isStatic(mods) || !Modifier.isAbstract(mods)) {
                // no need for recursive calls here, which are costy in interpreted mode!
                continue;
            }
            if (method.getParameterCount() > 0) {
                throw new IllegalArgumentException("Configuration methods cannot accept parameters");
            }
            if (method.getReturnType() == void.class) {
                throw new IllegalArgumentException("Void config methods are not allowed");
            }
            Property p = getPropertyDef(method, method.getAnnotatedReturnType());
            final Property[] array;
            if (i + 1 == methods.length) {
                array = new Property[ti + 1];
            } else {
                array = getProperties(methods, i + 1, ti + 1);
            }
            array[ti] = p;
            return array;
        }
        return ti > 0 ? new Property[ti] : NO_PROPERTIES;
    }

    private static Property getPropertyDef(Method method, AnnotatedType type) {
        if (isToStringMethod(method)) {
            return new ToStringMethod(method);
        }

        Method defaultMethod = hasDefaultMethodImplementation(method);
        if (defaultMethod != null) {
            return new DefaultMethodProperty(method, defaultMethod, getPropertyDef(defaultMethod, type));
        }

        // now figure out what kind it is
        Class<? extends Converter<?>> convertWith = getConverter(type, method);
        String propertyName = getPropertyName(method);
        Class<?> rawType = rawTypeOf(type.getType());
        if (rawType.isPrimitive()) {
            // primitive!
            return new PrimitiveProperty(method, propertyName, rawType, convertWith, getDefaultValue(method));
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
                AnnotatedType keyType = typeOfParameter(type, 0);
                AnnotatedType valueType = typeOfParameter(type, 1);
                String defaultValue = getDefaultValue(method);
                return new MapProperty(method, propertyName, keyType.getType(), getUnnamedKey(keyType, method),
                        getConverter(keyType, method), getPropertyDef(method, valueType),
                        defaultValue != null || hasDefaults(method), defaultValue);
            }
            if (rawType == List.class || rawType == Set.class) {
                AnnotatedType elementType = typeOfParameter(type, 0);

                if (rawTypeOf(elementType.getType()) == Map.class) {
                    return new CollectionProperty(rawType, getPropertyDef(method, elementType));
                }

                ConfigMappingInterface configurationInterface = getConfigurationInterface(rawTypeOf(elementType.getType()));
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

    static AnnotatedType typeOfParameter(final AnnotatedType type, final int index) {
        if (type instanceof AnnotatedParameterizedType) {
            return ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments()[index];
        } else {
            return type;
        }
    }

    static Type typeOfParameter(final Type type, final int index) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments()[index];
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
}
