package io.smallrye.config;

import static io.smallrye.config.ConfigMappingProvider.skewer;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.spi.Converter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.smallrye.common.classloader.ClassDefiner;
import io.smallrye.common.constraint.Assert;
import io.smallrye.config.inject.InjectionMessages;

/**
 * Information about a configuration interface.
 */
final class ConfigMappingInterface {
    static final ConfigMappingInterface[] NO_TYPES = new ConfigMappingInterface[0];
    static final Property[] NO_PROPERTIES = new Property[0];
    static final ClassValue<ConfigMappingInterface> cv = new ClassValue<ConfigMappingInterface>() {
        protected ConfigMappingInterface computeValue(final Class<?> type) {
            return createConfigurationInterface(type);
        }
    };
    static final boolean usefulDebugInfo;

    static {
        usefulDebugInfo = Boolean.parseBoolean(AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty("io.smallrye.config.mapper.useful-debug-info")));
    }

    private final Class<?> interfaceType;
    private final ConfigMappingInterface[] superTypes;
    private final Property[] properties;
    private final Constructor<? extends ConfigMappingObject> constructor;
    private final Map<String, Property> propertiesByName;

    ConfigMappingInterface(final Class<?> interfaceType, final ConfigMappingInterface[] superTypes,
            final Property[] properties) {
        this.interfaceType = interfaceType;
        this.superTypes = superTypes;
        this.properties = properties;
        try {
            constructor = createConfigurationObjectClass().asSubclass(ConfigMappingObject.class)
                    .getDeclaredConstructor(ConfigMappingContext.class);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(e.getMessage());
        }
        final Map<String, Property> propertiesByName = new HashMap<>();
        for (Property property : properties) {
            propertiesByName.put(property.getMethod().getName(), property);
        }
        this.propertiesByName = propertiesByName;
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
    public int getSuperTypeCount() {
        return superTypes.length;
    }

    /**
     * Get the supertype at the given index, which must be greater than or equal to zero and less than the value returned
     * by {@link #getSuperTypeCount()}.
     *
     * @param index the index
     * @return the supertype definition
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    public ConfigMappingInterface getSuperType(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= superTypes.length)
            throw new IndexOutOfBoundsException();
        return superTypes[index];
    }

    /**
     * Get the number of properties defined on this type (excluding supertypes).
     *
     * @return the number of properties
     */
    public int getPropertyCount() {
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
    public Property getProperty(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= properties.length)
            throw new IndexOutOfBoundsException();
        return properties[index];
    }

    public Property getProperty(final String name) {
        return propertiesByName.get(name);
    }

    Constructor<? extends ConfigMappingObject> getConstructor() {
        return constructor;
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
            return Assert.checkNotEmptyParam("propertyName", Assert.checkNotNullParam("propertyName", propertyName));
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

        int getReturnInstruction() {
            if (primitiveType == float.class) {
                return Opcodes.FRETURN;
            } else if (primitiveType == double.class) {
                return Opcodes.DRETURN;
            } else if (primitiveType == long.class) {
                return Opcodes.LRETURN;
            } else {
                return Opcodes.IRETURN;
            }
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

    static ConfigMappingInterface createConfigurationInterface(Class<?> interfaceType) {
        if (!interfaceType.isInterface() || interfaceType.getTypeParameters().length != 0) {
            return null;
        }
        // first, find any supertypes
        ConfigMappingInterface[] superTypes = getSuperTypes(interfaceType.getInterfaces(), 0, 0);
        // now find any properties
        Property[] properties = getProperties(interfaceType.getDeclaredMethods(), 0, 0);
        // is it anything?
        if (superTypes.length == 0 && properties.length == 0) {
            // no
            return null;
        } else {
            // it is a proper configuration interface
            return new ConfigMappingInterface(interfaceType, superTypes, properties);
        }
    }

    private static final String I_CLASS = getInternalName(Class.class);
    private static final String I_COLLECTIONS = getInternalName(Collections.class);
    private static final String I_CONFIGURATION_OBJECT = getInternalName(ConfigMappingObject.class);
    private static final String I_CONVERTER = getInternalName(Converter.class);
    private static final String I_MAP = getInternalName(Map.class);
    private static final String I_MAPPING_CONTEXT = getInternalName(ConfigMappingContext.class);
    private static final String I_OBJECT = getInternalName(Object.class);
    private static final String I_OPTIONAL = getInternalName(Optional.class);
    private static final String I_RUNTIME_EXCEPTION = getInternalName(RuntimeException.class);
    private static final String I_SMALLRYE_CONFIG = getInternalName(SmallRyeConfig.class);
    private static final String I_STRING_BUILDER = getInternalName(StringBuilder.class);
    private static final String I_STRING = getInternalName(String.class);

    private static final int V_THIS = 0;
    private static final int V_MAPPING_CONTEXT = 1;
    private static final int V_STRING_BUILDER = 2;
    private static final int V_LENGTH = 3;

    private void addProperties(ClassVisitor cv, final String className, MethodVisitor ctor, MethodVisitor fio,
            Set<String> visited) {
        for (Property property : properties) {
            Method method = property.getMethod();
            String memberName = method.getName();
            if (!visited.add(memberName)) {
                // duplicated property
                continue;
            }
            // the field
            String fieldType = getInternalName(method.getReturnType());
            String fieldDesc = getDescriptor(method.getReturnType());
            cv.visitField(Opcodes.ACC_PRIVATE, memberName, fieldDesc, null, null);

            // now process the property
            final Property realProperty;
            final boolean optional = property.isOptional();
            if (optional) {
                realProperty = property.asOptional().getNestedProperty();
            } else {
                realProperty = property;
            }

            // now handle each possible type
            if (property.isMap()) {
                // stack: -
                ctor.visitMethodInsn(Opcodes.INVOKESTATIC, I_COLLECTIONS, "emptyMap", "()L" + I_MAP + ';', false);
                // stack: map
                ctor.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: map this
                ctor.visitInsn(Opcodes.SWAP);
                // stack: this map
                ctor.visitFieldInsn(Opcodes.PUTFIELD, className, memberName, fieldDesc);
                // stack: -
                // then sweep it up
                // stack: -
                fio.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
                // stack: ctxt
                fio.visitLdcInsn(getType(interfaceType));
                // stack: ctxt iface
                fio.visitLdcInsn(memberName);
                // stack: ctxt iface name
                fio.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: ctxt iface name this
                fio.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getEnclosedField",
                        "(L" + I_CLASS + ";L" + I_STRING + ";L" + I_OBJECT + ";)L" + I_OBJECT + ';', false);
                // stack: obj?
                fio.visitInsn(Opcodes.DUP);
                Label _continue = new Label();
                Label _done = new Label();
                // stack: obj? obj?
                fio.visitJumpInsn(Opcodes.IFNULL, _continue);
                // stack: obj
                fio.visitTypeInsn(Opcodes.CHECKCAST, I_MAP);
                // stack: map
                fio.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: map this
                fio.visitInsn(Opcodes.SWAP);
                // stack: this map
                fio.visitFieldInsn(Opcodes.PUTFIELD, className, memberName, fieldDesc);
                // stack: -
                fio.visitJumpInsn(Opcodes.GOTO, _done);
                fio.visitLabel(_continue);
                // stack: null
                fio.visitInsn(Opcodes.POP);
                // stack: -
                fio.visitLabel(_done);
            } else if (property.isGroup()) {
                // stack: -
                boolean restoreLength = appendPropertyName(ctor, property, memberName);
                // stack: -
                ctor.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
                // stack: ctxt
                ctor.visitLdcInsn(getType(realProperty.asGroup().getGroupType().getInterfaceType()));
                // stack: ctxt clazz
                ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "constructGroup",
                        "(L" + I_CLASS + ";)L" + I_OBJECT + ';', false);
                // stack: nested
                ctor.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: nested this
                ctor.visitInsn(Opcodes.SWAP);
                // stack: this nested
                ctor.visitFieldInsn(Opcodes.PUTFIELD, className, memberName, fieldDesc);
                // stack: -
                if (restoreLength) {
                    restoreLength(ctor);
                }
            } else if (property.isLeaf() || property.isPrimitive() || property.isOptional() && property.isLeaf()) {
                // stack: -
                ctor.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: this
                boolean restoreLength = appendPropertyName(ctor, property, memberName);
                ctor.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
                ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getConfig", "()L" + I_SMALLRYE_CONFIG + ';',
                        false);
                // stack: this config
                ctor.visitVarInsn(Opcodes.ALOAD, V_STRING_BUILDER);
                ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_STRING_BUILDER, "toString", "()L" + I_STRING + ';', false);
                // stack: this config key
                // get the converter to use
                ctor.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
                //    public <T> Converter<T> getValueConverter(Class<?> enclosingType, String field) {
                ctor.visitLdcInsn(getType(getInterfaceType()));
                ctor.visitLdcInsn(memberName);
                ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getValueConverter",
                        "(L" + I_CLASS + ";L" + I_STRING + ";)L" + I_CONVERTER + ';', false);
                // stack: this config key converter
                Label _try = new Label();
                Label _catch = new Label();
                Label _continue = new Label();
                ctor.visitLabel(_try);
                if (property.isOptional()) {
                    ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_SMALLRYE_CONFIG, "getOptionalValue",
                            "(L" + I_STRING + ";L" + I_CONVERTER + ";)L" + I_OPTIONAL + ';', false);
                } else {
                    ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_SMALLRYE_CONFIG, "getValue",
                            "(L" + I_STRING + ";L" + I_CONVERTER + ";)L" + I_OBJECT + ';', false);
                }
                // stack: this value
                if (property.isPrimitive()) {
                    PrimitiveProperty prim = property.asPrimitive();
                    // unbox it
                    // stack: this box
                    String boxType = getInternalName(prim.getBoxType());
                    ctor.visitTypeInsn(Opcodes.CHECKCAST, boxType);
                    ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, boxType, prim.getUnboxMethodName(),
                            prim.getUnboxMethodDescriptor(), false);
                    // stack: this value
                } else if (!property.isOptional()) {
                    assert property.isLeaf();
                    ctor.visitTypeInsn(Opcodes.CHECKCAST, fieldType);
                }
                // stack: this value
                ctor.visitFieldInsn(Opcodes.PUTFIELD, className, memberName, fieldDesc);
                // stack: -
                ctor.visitJumpInsn(Opcodes.GOTO, _continue);
                ctor.visitLabel(_catch);
                // stack: exception
                ctor.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
                // stack: exception ctxt
                ctor.visitInsn(Opcodes.SWAP);
                // stack: ctxt exception
                ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "reportProblem",
                        "(L" + I_RUNTIME_EXCEPTION + ";)V", false);
                // stack: -
                ctor.visitLabel(_continue);
                if (restoreLength) {
                    restoreLength(ctor);
                }
                // add the try/catch
                ctor.visitTryCatchBlock(_try, _catch, _catch, I_RUNTIME_EXCEPTION);
            } else if (property.isOptional()) {
                // stack: -
                ctor.visitMethodInsn(Opcodes.INVOKESTATIC, I_OPTIONAL, "empty", "()L" + I_OPTIONAL + ";", false);
                // stack: empty
                ctor.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: empty this
                ctor.visitInsn(Opcodes.SWAP);
                // stack: this empty
                ctor.visitFieldInsn(Opcodes.PUTFIELD, className, memberName, fieldDesc);

                // also generate a sweep-up stub
                // stack: -
                fio.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
                // stack: ctxt
                fio.visitLdcInsn(getType(interfaceType));
                // stack: ctxt iface
                fio.visitLdcInsn(memberName);
                // stack: ctxt iface name
                fio.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: ctxt iface name this
                fio.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getEnclosedField",
                        "(L" + I_CLASS + ";L" + I_STRING + ";L" + I_OBJECT + ";)L" + I_OBJECT + ';', false);
                // stack: obj?
                fio.visitInsn(Opcodes.DUP);
                Label _continue = new Label();
                Label _done = new Label();
                // stack: obj? obj?
                fio.visitJumpInsn(Opcodes.IFNULL, _continue);
                // stack: obj
                fio.visitMethodInsn(Opcodes.INVOKESTATIC, I_OPTIONAL, "of", "(L" + I_OBJECT + ";)L" + I_OPTIONAL + ';', false);
                // stack: opt
                fio.visitVarInsn(Opcodes.ALOAD, V_THIS);
                // stack: opt this
                fio.visitInsn(Opcodes.SWAP);
                // stack: this opt
                fio.visitFieldInsn(Opcodes.PUTFIELD, className, memberName, fieldDesc);
                // stack: -
                fio.visitJumpInsn(Opcodes.GOTO, _done);
                fio.visitLabel(_continue);
                // stack: null
                fio.visitInsn(Opcodes.POP);
                // stack: -
                fio.visitLabel(_done);
            }

            // the accessor method implementation
            MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, memberName, "()" + fieldDesc, null, null);
            // stack: -
            mv.visitVarInsn(Opcodes.ALOAD, V_THIS);
            // stack: this
            mv.visitFieldInsn(Opcodes.GETFIELD, className, memberName, fieldDesc);
            // stack: obj
            if (property.isPrimitive()) {
                mv.visitInsn(property.asPrimitive().getReturnInstruction());
            } else {
                mv.visitInsn(Opcodes.ARETURN);
            }
            mv.visitEnd();
            mv.visitMaxs(0, 0);
            // end loop
        }
        // subtype overrides supertype
        for (ConfigMappingInterface superType : superTypes) {
            superType.addProperties(cv, className, ctor, fio, visited);
        }
    }

    private boolean appendPropertyName(final MethodVisitor ctor, final Property property, final String memberName) {
        if (property.isParentPropertyName()) {
            return false;
        }
        // stack: -
        Label _continue = new Label();

        ctor.visitVarInsn(Opcodes.ALOAD, V_STRING_BUILDER);

        ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_STRING_BUILDER, "length", "()I", false);
        // if length != 0 (mean that a prefix exists and not the empty prefix)
        ctor.visitJumpInsn(Opcodes.IFEQ, _continue);

        ctor.visitVarInsn(Opcodes.ALOAD, V_STRING_BUILDER);
        // stack: sb
        ctor.visitLdcInsn('.');
        // stack: sb '.'
        ctor.visitInsn(Opcodes.I2C);
        // stack: sb '.'
        ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(C)L" + I_STRING_BUILDER + ';', false);

        ctor.visitInsn(Opcodes.POP);

        ctor.visitLabel(_continue);

        ctor.visitVarInsn(Opcodes.ALOAD, V_STRING_BUILDER);

        // stack: sb
        if (property.hasPropertyName()) {
            ctor.visitLdcInsn(property.getPropertyName());
        } else {
            ctor.visitLdcInsn(skewer(memberName));
        }
        // stack: sb name
        ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_STRING_BUILDER, "append",
                "(L" + I_STRING + ";)L" + I_STRING_BUILDER + ';', false);
        // stack: sb
        ctor.visitInsn(Opcodes.POP);
        // stack: -
        return true;
    }

    private void restoreLength(final MethodVisitor ctor) {
        // stack: -
        ctor.visitVarInsn(Opcodes.ALOAD, V_STRING_BUILDER);
        // stack: sb
        ctor.visitVarInsn(Opcodes.ILOAD, V_LENGTH);
        // stack: sb length
        ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_STRING_BUILDER, "setLength", "(I)V", false);
        // stack: -
    }

    Class<?> createConfigurationObjectClass() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = usefulDebugInfo ? new Debugging.ClassVisitorImpl(writer) : writer;

        String interfacePackage = interfaceType.getPackage().getName();
        String className = getClass().getPackage().getName() + "." + interfaceType.getSimpleName() +
                interfacePackage.hashCode() + "Impl";
        String classInternalName = className.replace('.', '/');

        visitor.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, classInternalName, null, I_OBJECT, new String[] {
                I_CONFIGURATION_OBJECT,
                getInternalName(interfaceType)
        });
        visitor.visitSource(null, null);
        MethodVisitor ctor = visitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(L" + I_MAPPING_CONTEXT + ";)V", null, null);
        ctor.visitParameter("context", Opcodes.ACC_FINAL);
        Label ctorStart = new Label();
        Label ctorEnd = new Label();
        ctor.visitLabel(ctorStart);
        // stack: -
        ctor.visitVarInsn(Opcodes.ALOAD, V_THIS);
        // stack: this
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);
        // stack: -
        ctor.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
        // stack: ctxt
        ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getStringBuilder", "()L" + I_STRING_BUILDER + ';',
                false);
        // stack: sb
        ctor.visitInsn(Opcodes.DUP);
        // stack: sb sb
        Label ctorSbStart = new Label();
        ctor.visitLabel(ctorSbStart);
        ctor.visitVarInsn(Opcodes.ASTORE, V_STRING_BUILDER);
        // stack: sb
        ctor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_STRING_BUILDER, "length", "()I", false);
        // stack: len
        Label ctorLenStart = new Label();
        ctor.visitLabel(ctorLenStart);
        ctor.visitVarInsn(Opcodes.ISTORE, V_LENGTH);
        // stack: -
        MethodVisitor fio = visitor.visitMethod(Opcodes.ACC_PUBLIC, "fillInOptionals", "(L" + I_MAPPING_CONTEXT + ";)V", null,
                null);
        fio.visitParameter("context", Opcodes.ACC_FINAL);
        Label fioStart = new Label();
        Label fioEnd = new Label();
        fio.visitLabel(fioStart);
        // stack: -
        fio.visitVarInsn(Opcodes.ALOAD, V_MAPPING_CONTEXT);
        // stack: ctxt
        fio.visitMethodInsn(Opcodes.INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getStringBuilder", "()L" + I_STRING_BUILDER + ';',
                false);
        // stack: sb
        fio.visitVarInsn(Opcodes.ASTORE, V_STRING_BUILDER);
        // stack: -
        addProperties(visitor, classInternalName, ctor, fio, new HashSet<>());
        // stack: -
        fio.visitInsn(Opcodes.RETURN);
        fio.visitLabel(fioEnd);
        fio.visitLocalVariable("mc", 'L' + I_MAPPING_CONTEXT + ';', null, fioStart, fioEnd, V_MAPPING_CONTEXT);
        fio.visitEnd();
        fio.visitMaxs(0, 0);
        // stack: -
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitLabel(ctorEnd);
        ctor.visitLocalVariable("mc", 'L' + I_MAPPING_CONTEXT + ';', null, ctorStart, ctorEnd, V_MAPPING_CONTEXT);
        ctor.visitLocalVariable("sb", 'L' + I_STRING_BUILDER + ';', null, ctorSbStart, ctorEnd, V_STRING_BUILDER);
        ctor.visitLocalVariable("len", "I", null, ctorLenStart, ctorEnd, V_LENGTH);
        ctor.visitEnd();
        ctor.visitMaxs(0, 0);
        visitor.visitEnd();

        return ClassDefiner.defineClass(MethodHandles.lookup(), getClass(), className, writer.toByteArray());
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

    private static Property[] getProperties(Method[] methods, int si, int ti) {
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
        // now figure out what kind it is
        Class<? extends Converter<?>> convertWith = getConvertWith(type);
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
            ConfigMappingInterface configurationInterface = getConfigurationInterface(rawType);
            if (configurationInterface != null) {
                // it's a group
                return new GroupProperty(method, propertyName, configurationInterface);
            }
            // fall out (leaf)
        }
        // otherwise it's a leaf
        WithDefault annotation = method.getAnnotation(WithDefault.class);
        return new LeafProperty(method, propertyName, type, convertWith, annotation == null ? null : annotation.value());
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
            throw InjectionMessages.msg.noRawType(type);
        }
    }

    static final class Debugging {
        static StackTraceElement getCaller() {
            return new Throwable().getStackTrace()[2];
        }

        static final class MethodVisitorImpl extends MethodVisitor {

            MethodVisitorImpl(final int api) {
                super(api);
            }

            MethodVisitorImpl(final int api, final MethodVisitor methodVisitor) {
                super(api, methodVisitor);
            }

            public void visitInsn(final int opcode) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitInsn(opcode);
            }

            public void visitIntInsn(final int opcode, final int operand) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitIntInsn(opcode, operand);
            }

            public void visitVarInsn(final int opcode, final int var) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitVarInsn(opcode, var);
            }

            public void visitTypeInsn(final int opcode, final String type) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitTypeInsn(opcode, type);
            }

            public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }

            public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitMethodInsn(opcode, owner, name, descriptor);
            }

            public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor,
                    final boolean isInterface) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }

            public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle,
                    final Object... bootstrapMethodArguments) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            }

            public void visitJumpInsn(final int opcode, final Label label) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitJumpInsn(opcode, label);
            }

            public void visitLdcInsn(final Object value) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitLdcInsn(value);
            }

            public void visitIincInsn(final int var, final int increment) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitIincInsn(var, increment);
            }

            public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label... labels) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitTableSwitchInsn(min, max, dflt, labels);
            }

            public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitLookupSwitchInsn(dflt, keys, labels);
            }

            public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
                Label l = new Label();
                visitLabel(l);
                visitLineNumber(getCaller().getLineNumber(), l);
                super.visitMultiANewArrayInsn(descriptor, numDimensions);
            }
        }

        static final class ClassVisitorImpl extends ClassVisitor {

            final String sourceFile;

            ClassVisitorImpl(final int api) {
                super(api);
                sourceFile = getCaller().getFileName();
            }

            ClassVisitorImpl(final ClassWriter cw) {
                super(Opcodes.ASM7, cw);
                sourceFile = getCaller().getFileName();
            }

            public void visitSource(final String source, final String debug) {
                super.visitSource(sourceFile, debug);
            }

            public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                    final String signature,
                    final String[] exceptions) {
                return new MethodVisitorImpl(api, super.visitMethod(access, name, descriptor, signature, exceptions));
            }
        }
    }
}
