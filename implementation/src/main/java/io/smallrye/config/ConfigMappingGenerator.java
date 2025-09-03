package io.smallrye.config;

import static io.smallrye.config.ConfigMappingInterface.ConfigMappingBuilder.getBuilderClassName;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM7;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DCMPL;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappingContext.ObjectCreator;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
import io.smallrye.config.ConfigMappingInterface.GroupProperty;
import io.smallrye.config.ConfigMappingInterface.LeafProperty;
import io.smallrye.config.ConfigMappingInterface.MapProperty;
import io.smallrye.config.ConfigMappingInterface.MayBeOptionalProperty;
import io.smallrye.config.ConfigMappingInterface.PrimitiveProperty;
import io.smallrye.config.ConfigMappingInterface.Property;

public class ConfigMappingGenerator {
    static final boolean usefulDebugInfo;
    /**
     * The regular expression allowing to detect arrays in a full type name.
     */
    private static final Pattern ARRAY_FORMAT_REGEX = Pattern.compile("([<;])L(.*)\\[];");

    static {
        usefulDebugInfo = Boolean.parseBoolean(AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty("io.smallrye.config.mapper.useful-debug-info")));
    }

    private static final String I_CLASS = getInternalName(Class.class);
    private static final String I_FIELD = getInternalName(Field.class);

    private static final String I_MAPPING_CONTEXT = getInternalName(ConfigMappingContext.class);
    private static final String I_OBJECT_CREATOR = getInternalName(ConfigMappingContext.ObjectCreator.class);
    private static final String I_NAMING_STRATEGY = getInternalName(NamingStrategy.class);
    private static final String I_STRING_BUILDER = getInternalName(StringBuilder.class);

    private static final String I_RUNTIME_EXCEPTION = getInternalName(RuntimeException.class);
    private static final String I_OBJECT = getInternalName(Object.class);
    private static final String I_STRING = getInternalName(String.class);
    private static final String I_ITERABLE = getInternalName(Iterable.class);
    private static final String I_COLLECTION = getInternalName(Collection.class);

    private static final int V_THIS = 0;
    private static final int V_MAPPING_CONTEXT = 1;

    /**
     * Generates the backing implementation of an interface annotated with the {@link ConfigMapping} annotation.
     *
     * @param mapping information about a configuration interface.
     * @return the class bytes representing the implementation of the configuration interface.
     */
    static byte[] generate(final ConfigMappingInterface mapping) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = usefulDebugInfo ? new Debugging.ClassVisitorImpl(writer) : writer;

        visitor.visit(V1_8, ACC_PUBLIC, mapping.getClassInternalName(), null, I_OBJECT,
                new String[] { getInternalName(mapping.getInterfaceType()) });
        visitor.visitSource(null, null);

        // No Args Constructor - To use for proxies
        MethodVisitor noArgsCtor = visitor.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        noArgsCtor.visitVarInsn(ALOAD, V_THIS);
        noArgsCtor.visitMethodInsn(INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);
        noArgsCtor.visitInsn(RETURN);
        noArgsCtor.visitEnd();
        noArgsCtor.visitMaxs(0, 0);

        // Builder Constructor
        String builderName = getBuilderClassName(mapping.getInterfaceType()).replace('.', '/');
        MethodVisitor builderCtor = visitor.visitMethod(ACC_PUBLIC, "<init>", "(L" + builderName + ";)V", null, null);
        builderCtor.visitVarInsn(ALOAD, V_THIS);
        builderCtor.visitMethodInsn(INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);
        for (Property property : mapping.getProperties()) {
            Method method = property.getMethod();
            String memberName = method.getName();
            String fieldDesc = getDescriptor(method.getReturnType());
            builderCtor.visitVarInsn(ALOAD, V_THIS);
            builderCtor.visitVarInsn(ALOAD, 1);
            builderCtor.visitFieldInsn(GETFIELD, builderName, memberName, fieldDesc);
            builderCtor.visitFieldInsn(PUTFIELD, mapping.getClassInternalName(), memberName, fieldDesc);
        }
        builderCtor.visitInsn(RETURN);
        builderCtor.visitEnd();
        builderCtor.visitMaxs(0, 0);

        ObjectCreatorMethodVisitor ctor = new ObjectCreatorMethodVisitor(
                visitor.visitMethod(ACC_PUBLIC, "<init>", "(L" + I_MAPPING_CONTEXT + ";)V", null, null));
        ctor.visitParameter("context", ACC_FINAL);
        Label ctorStart = new Label();
        ctor.visitLabel(ctorStart);
        ctor.visitVarInsn(ALOAD, V_THIS);
        ctor.visitMethodInsn(INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);

        if (mapping.hasConfigMapping()) {
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitFieldInsn(GETSTATIC, I_NAMING_STRATEGY, mapping.getNamingStrategy().name(),
                    "L" + I_NAMING_STRATEGY + ";");
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_MAPPING_CONTEXT, "applyNamingStrategy", "(L" + I_NAMING_STRATEGY + ";)V",
                    false);

            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitInsn(mapping.isBeanStyleGetters() ? ICONST_1 : ICONST_0);
            ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_MAPPING_CONTEXT, "applyBeanStyleGetters", "(Ljava/lang/Boolean;)V", false);
        }

        addProperties(visitor, ctor, mapping);

        ctor.visitInsn(RETURN);
        Label ctorEnd = new Label();
        ctor.visitLabel(ctorEnd);
        ctor.visitLocalVariable("mc", 'L' + I_MAPPING_CONTEXT + ';', null, ctorStart, ctorEnd, V_MAPPING_CONTEXT);
        ctor.visitEnd();
        ctor.visitMaxs(0, 0);
        visitor.visitEnd();

        generateStaticInit(visitor, mapping);
        generateEquals(visitor, mapping);
        generateHashCode(visitor, mapping);
        generateToString(visitor, mapping);

        return writer.toByteArray();
    }

    private static final String I_CONFIG_INSTANCE_BUILDER = getInternalName(ConfigInstanceBuilder.class);
    private static final String I_CONFIG_INSTANCE_BUILDER_IMPL = getInternalName(ConfigInstanceBuilderImpl.class);

    static byte[] generateBuilder(final ConfigMappingInterface mapping, final String builderClassName) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = usefulDebugInfo ? new Debugging.ClassVisitorImpl(writer) : writer;

        visitor.visit(V1_8, ACC_PUBLIC, builderClassName, null, I_OBJECT, new String[] {});
        visitor.visitSource(null, null);

        // No Args Constructor
        MethodVisitor noArgsCtor = visitor.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        noArgsCtor.visitVarInsn(ALOAD, V_THIS);
        noArgsCtor.visitMethodInsn(INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);
        for (Property property : mapping.getProperties()) {
            if ((property.isLeaf() || property.isPrimitive()) && property.hasDefaultValue()
                    && property.getDefaultValue() != null) {
                noArgsCtor.visitVarInsn(ALOAD, V_THIS);
                noArgsCtor.visitLdcInsn(property.getDefaultValue());
                if (property.isPrimitive()) {
                    PrimitiveProperty primitive = property.asPrimitive();
                    noArgsCtor.visitLdcInsn(Type.getType(getDescriptor(primitive.getBoxType())));
                } else {
                    noArgsCtor.visitLdcInsn(Type.getType(getDescriptor(property.getMethod().getReturnType())));
                }
                noArgsCtor.visitMethodInsn(INVOKESTATIC, I_CONFIG_INSTANCE_BUILDER_IMPL, "convertValue",
                        "(L" + I_STRING + ";L" + I_CLASS + ";)L" + I_OBJECT + ";", false);
                if (property.isPrimitive()) {
                    PrimitiveProperty primitive = property.asPrimitive();
                    noArgsCtor.visitTypeInsn(CHECKCAST, getInternalName(primitive.getBoxType()));
                    noArgsCtor.visitMethodInsn(INVOKEVIRTUAL, getInternalName(primitive.getBoxType()),
                            primitive.getUnboxMethodName(),
                            primitive.getUnboxMethodDescriptor(), false);
                } else {
                    noArgsCtor.visitTypeInsn(CHECKCAST, getInternalName(property.getMethod().getReturnType()));
                }
                noArgsCtor.visitFieldInsn(PUTFIELD, builderClassName, property.getMethod().getName(),
                        getDescriptor(property.getMethod().getReturnType()));
            } else if (property.isGroup()) {
                noArgsCtor.visitVarInsn(ALOAD, V_THIS);
                noArgsCtor.visitLdcInsn(Type.getType(getDescriptor(property.getMethod().getReturnType())));
                noArgsCtor.visitMethodInsn(INVOKESTATIC, I_CONFIG_INSTANCE_BUILDER, "forInterface",
                        "(L" + I_CLASS + ";)L" + I_CONFIG_INSTANCE_BUILDER + ";", true);
                noArgsCtor.visitMethodInsn(INVOKEINTERFACE, I_CONFIG_INSTANCE_BUILDER, "build", "()L" + I_OBJECT + ";", true);
                noArgsCtor.visitTypeInsn(CHECKCAST, getInternalName(property.getMethod().getReturnType()));
                noArgsCtor.visitFieldInsn(PUTFIELD, builderClassName, property.getMethod().getName(),
                        getDescriptor(property.getMethod().getReturnType()));
            } else if (property.isCollection() && property.asCollection().getElement().isLeaf()) {
                CollectionProperty collectionProperty = property.asCollection();
                LeafProperty elementProperty = collectionProperty.getElement().asLeaf();
                if (elementProperty.hasDefaultValue() && elementProperty.getDefaultValue() != null) {
                    noArgsCtor.visitVarInsn(ALOAD, V_THIS);
                    noArgsCtor.visitLdcInsn(elementProperty.getDefaultValue());
                    noArgsCtor.visitLdcInsn(Type.getType(getDescriptor(elementProperty.getValueRawType())));
                    noArgsCtor.visitLdcInsn(Type.getType(getDescriptor(collectionProperty.getCollectionRawType())));
                    noArgsCtor.visitMethodInsn(INVOKESTATIC, I_CONFIG_INSTANCE_BUILDER_IMPL, "convertValues",
                            "(L" + I_STRING + ";L" + I_CLASS + ";L" + I_CLASS + ";)L" + I_COLLECTION + ";", false);
                    noArgsCtor.visitTypeInsn(CHECKCAST, getInternalName(property.getMethod().getReturnType()));
                    noArgsCtor.visitFieldInsn(PUTFIELD, builderClassName, property.getMethod().getName(),
                            getDescriptor(property.getMethod().getReturnType()));
                }
            } else if (property.isMap()) {
                MapProperty mapProperty = property.asMap();
                if (mapProperty.getValueProperty().isLeaf() && mapProperty.hasDefaultValue()
                        && mapProperty.getDefaultValue() != null) {
                    noArgsCtor.visitVarInsn(ALOAD, V_THIS);
                    noArgsCtor.visitTypeInsn(NEW, I_CONFIG_INSTANCE_BUILDER_IMPL + "$MapWithDefault");
                    noArgsCtor.visitInsn(DUP);
                    noArgsCtor.visitLdcInsn(mapProperty.getDefaultValue());
                    noArgsCtor.visitMethodInsn(INVOKESPECIAL, I_CONFIG_INSTANCE_BUILDER_IMPL + "$MapWithDefault", "<init>",
                            "(L" + I_OBJECT + ";)V", false);
                    noArgsCtor.visitFieldInsn(PUTFIELD, builderClassName, property.getMethod().getName(),
                            getDescriptor(property.getMethod().getReturnType()));
                } else if (mapProperty.getValueProperty().isGroup()) {
                    GroupProperty groupProperty = mapProperty.getValueProperty().asGroup();
                    noArgsCtor.visitVarInsn(ALOAD, V_THIS);
                    noArgsCtor.visitTypeInsn(NEW, I_CONFIG_INSTANCE_BUILDER_IMPL + "$MapWithDefault");
                    noArgsCtor.visitInsn(DUP);
                    noArgsCtor.visitLdcInsn(getType(groupProperty.getGroupType().getInterfaceType()));
                    noArgsCtor.visitMethodInsn(INVOKESTATIC, I_CONFIG_INSTANCE_BUILDER, "forInterface",
                            "(L" + I_CLASS + ";)L" + I_CONFIG_INSTANCE_BUILDER + ";", true);
                    noArgsCtor.visitMethodInsn(INVOKEINTERFACE, I_CONFIG_INSTANCE_BUILDER, "build", "()L" + I_OBJECT + ";",
                            true);
                    noArgsCtor.visitTypeInsn(CHECKCAST, getInternalName(groupProperty.getGroupType().getInterfaceType()));
                    noArgsCtor.visitMethodInsn(INVOKESPECIAL, I_CONFIG_INSTANCE_BUILDER_IMPL + "$MapWithDefault", "<init>",
                            "(L" + I_OBJECT + ";)V", false);
                    noArgsCtor.visitFieldInsn(PUTFIELD, builderClassName, property.getMethod().getName(),
                            getDescriptor(property.getMethod().getReturnType()));
                }
            }
        }
        noArgsCtor.visitInsn(RETURN);
        noArgsCtor.visitEnd();
        noArgsCtor.visitMaxs(0, 0);

        for (Property property : mapping.getProperties()) {
            Method method = property.getMethod();
            String memberName = method.getName();

            // Field Declaration
            String fieldDesc = getDescriptor(method.getReturnType());
            // TODO - Should it be public? And use field access to copy from the builder to the config class?
            visitor.visitField(ACC_PUBLIC, memberName, fieldDesc, null, null);

            // Setter
            MethodVisitor mv = visitor.visitMethod(ACC_PUBLIC, memberName, "(" + fieldDesc + ")V", null, null);
            mv.visitVarInsn(ALOAD, V_THIS);
            switch (Type.getReturnType(method).getSort()) {
                case Type.BOOLEAN,
                        Type.SHORT,
                        Type.CHAR,
                        Type.BYTE,
                        Type.INT ->
                    mv.visitVarInsn(ILOAD, 1);

                case Type.LONG -> mv.visitVarInsn(LLOAD, 1);

                case Type.FLOAT -> mv.visitVarInsn(FLOAD, 1);

                case Type.DOUBLE -> mv.visitVarInsn(DLOAD, 1);

                default -> mv.visitVarInsn(ALOAD, 1);
            }
            mv.visitFieldInsn(PUTFIELD, builderClassName, memberName, fieldDesc);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return writer.toByteArray();
    }

    /**
     * Generates a configuration interface to act as a middle ground between a configuration class and the backing
     * implementation of a configuration interface.
     * <p>
     *
     * MicroProfile Config 2.0 added support for configuration classes and the
     * {@link org.eclipse.microprofile.config.inject.ConfigProperties} annotation. Since SmallRye Config only supports
     * configuration interfaces, the expected interface is generated from the MicroProfile Config configuration
     * class annotated with {@link org.eclipse.microprofile.config.inject.ConfigProperties}.
     * <p>
     *
     * The generated configuration interface implements {@link ConfigMappingClassMapper} which provides the bridge
     * between the instance of the configuration class and the implementation of the configuration interface provided by
     * {@link ConfigMappingGenerator#generate(ConfigMappingInterface)} to retrieve the configuration values.
     *
     * @param classType the configuration class.
     * @param interfaceName the generated interface class name.
     * @return the class bytes representing the interface of the configuration class.
     */
    static byte[] generate(final Class<?> classType, final String interfaceName) {
        String classInternalName = getInternalName(classType);
        String interfaceInternalName = interfaceName.replace('.', '/');

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_8, ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, interfaceInternalName, null, I_OBJECT,
                new String[] { getInternalName(ConfigMappingClassMapper.class) });

        {
            AnnotationVisitor av = writer.visitAnnotation("L" + getInternalName(ConfigMapping.class) + ";", true);
            av.visitEnum("namingStrategy", "L" + getInternalName(NamingStrategy.class) + ";",
                    NamingStrategy.VERBATIM.toString());

            if (classType.isAnnotationPresent(ConfigProperties.class)) {
                av.visit("prefix", classType.getAnnotation(ConfigProperties.class).prefix());
            }

            av.visitEnd();
        }

        Object classInstance;
        try {
            classInstance = classType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Field[] declaredFields = classType.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, declaredField.getName(),
                    getMethodDescriptor(getType(declaredField.getType())), getSignature(declaredField),
                    null);

            boolean hasDefault = false;

            if (declaredField.isAnnotationPresent(WithName.class)) {
                AnnotationVisitor av = mv.visitAnnotation("L" + getInternalName(WithName.class) + ";", true);
                av.visit("value", declaredField.getAnnotation(WithName.class).value());
                av.visitEnd();
            }

            if (declaredField.isAnnotationPresent(WithDefault.class)) {
                AnnotationVisitor av = mv.visitAnnotation("L" + getInternalName(WithDefault.class) + ";", true);
                av.visit("value", declaredField.getAnnotation(WithDefault.class).value());
                av.visitEnd();
                hasDefault = true;
            }

            if (declaredField.isAnnotationPresent(WithConverter.class)) {
                AnnotationVisitor av = mv.visitAnnotation("L" + getInternalName(WithConverter.class) + ";", true);
                av.visit("value", declaredField.getAnnotation(WithConverter.class).value());
                av.visitEnd();
            }

            if (declaredField.isAnnotationPresent(ConfigProperty.class)) {
                ConfigProperty configProperty = declaredField.getAnnotation(ConfigProperty.class);
                {
                    if (!configProperty.name().isEmpty()) {
                        AnnotationVisitor av = mv.visitAnnotation("L" + getInternalName(WithName.class) + ";", true);
                        av.visit("value", configProperty.name());
                        av.visitEnd();
                    }
                }
                {
                    if (!configProperty.defaultValue().equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        AnnotationVisitor av = mv.visitAnnotation("L" + getInternalName(WithDefault.class) + ";", true);
                        av.visit("value", configProperty.defaultValue());
                        av.visitEnd();
                        hasDefault = true;
                    }
                }
            }

            if (!hasDefault) {
                try {
                    declaredField.setAccessible(true);
                    Object defaultValue = declaredField.get(classInstance);
                    if (hasDefaultValue(declaredField.getType(), defaultValue)) {
                        AnnotationVisitor av = mv.visitAnnotation("L" + getInternalName(WithDefault.class) + ";", true);
                        av.visit("value", defaultValue.toString());
                        av.visitEnd();
                    }
                } catch (IllegalAccessException e) {
                    // Ignore
                }
            }

            mv.visitEnd();
        }

        MethodVisitor ctor = writer.visitMethod(ACC_PUBLIC, "map", "()L" + I_OBJECT + ";", null, null);
        Label ctorStart = new Label();
        ctor.visitLabel(ctorStart);
        ctor.visitTypeInsn(NEW, classInternalName);
        ctor.visitInsn(DUP);
        ctor.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", "()V", false);
        ctor.visitVarInsn(ASTORE, 1);

        for (Field declaredField : declaredFields) {
            if (Modifier.isStatic(declaredField.getModifiers()) || Modifier.isVolatile(declaredField.getModifiers())
                    || Modifier.isFinal(declaredField.getModifiers())) {
                continue;
            }

            String name = declaredField.getName();
            Class<?> type = declaredField.getType();

            if (Modifier.isPublic(declaredField.getModifiers())) {
                ctor.visitVarInsn(ALOAD, 1);
                ctor.visitVarInsn(ALOAD, 0);
                ctor.visitMethodInsn(INVOKEINTERFACE, interfaceInternalName, name, getMethodDescriptor(getType(type)),
                        true);
                ctor.visitFieldInsn(PUTFIELD, classInternalName, name, getDescriptor(type));
            } else {
                ctor.visitLdcInsn(getType(classType));
                ctor.visitLdcInsn(name);
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_CLASS, "getDeclaredField",
                        getMethodDescriptor(getType(Field.class), getType(String.class)), false);
                ctor.visitVarInsn(ASTORE, 2);
                ctor.visitVarInsn(ALOAD, 2);
                ctor.visitInsn(ICONST_1);
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_FIELD, "setAccessible", "(Z)V", false);

                ctor.visitVarInsn(ALOAD, 2);
                ctor.visitVarInsn(ALOAD, 1);
                ctor.visitVarInsn(ALOAD, 0);
                ctor.visitMethodInsn(INVOKEINTERFACE, interfaceInternalName, name, getMethodDescriptor(getType(type)), true);

                switch (Type.getType(type).getSort()) {
                    case Type.BOOLEAN:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                        break;
                    case Type.BYTE:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                        break;
                    case Type.CHAR:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                        break;
                    case Type.SHORT:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                        break;
                    case Type.INT:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                        break;
                    case Type.FLOAT:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                        break;
                    case Type.LONG:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                        break;
                    case Type.DOUBLE:
                        ctor.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                        break;
                }

                ctor.visitMethodInsn(INVOKEVIRTUAL, I_FIELD, "set",
                        getMethodDescriptor(getType(void.class), getType(Object.class), getType(Object.class)), false);
            }
        }

        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitInsn(ARETURN);
        ctor.visitMaxs(2, 2);
        writer.visitEnd();

        return writer.toByteArray();
    }

    private static void addProperties(
            final ClassVisitor cv,
            final ObjectCreatorMethodVisitor ctor,
            final ConfigMappingInterface mapping) {

        for (Property property : mapping.getProperties()) {
            Method method = property.getMethod();
            String memberName = method.getName();

            // Field Declaration
            String fieldType = getInternalName(method.getReturnType());
            String fieldDesc = getDescriptor(method.getReturnType());
            cv.visitField(ACC_PRIVATE, memberName, fieldDesc, null, null);

            // Getter
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, memberName, "()" + fieldDesc, null, null);
            mv.visitVarInsn(ALOAD, V_THIS);
            mv.visitFieldInsn(GETFIELD, mapping.getClassInternalName(), memberName, fieldDesc);
            mv.visitInsn(getReturnInstruction(property));
            mv.visitEnd();
            mv.visitMaxs(0, 0);

            if (property.isDefaultMethod()) {
                continue;
            }

            // Constructor field init
            Label _try = new Label();
            Label _catch = new Label();
            Label _continue = new Label();
            ctor.visitTryCatchBlock(_try, _catch, _catch, I_RUNTIME_EXCEPTION);

            // try
            ctor.visitLabel(_try);

            ctor.visitVarInsn(ALOAD, V_THIS);
            generateProperty(ctor, property);

            ctor.visitCast(property, fieldType);
            ctor.visitFieldInsn(PUTFIELD, mapping.getClassInternalName(), memberName, fieldDesc);
            ctor.visitJumpInsn(GOTO, _continue);

            // catch
            ctor.visitLabel(_catch);
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitInsn(SWAP);
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_MAPPING_CONTEXT, "problem", "(L" + I_RUNTIME_EXCEPTION + ";)V", false);
            ctor.visitJumpInsn(GOTO, _continue);

            ctor.visitLabel(_continue);
        }

        // We don't know the order in the constructor and the default method may require call to other
        // properties that may not be initialized yet, so we add them last
        for (Property property : mapping.getProperties()) {
            Method method = property.getMethod();
            String memberName = method.getName();
            String fieldDesc = getDescriptor(method.getReturnType());

            if (property.isDefaultMethod()) {
                ctor.visitVarInsn(ALOAD, V_THIS);
                Method defaultMethod = property.asDefaultMethod().getDefaultMethod();
                ctor.visitVarInsn(ALOAD, V_THIS);
                ctor.visitMethodInsn(INVOKESTATIC, getInternalName(defaultMethod.getDeclaringClass()), defaultMethod.getName(),
                        "(" + getType(mapping.getInterfaceType()) + ")" + fieldDesc, false);
                ctor.visitFieldInsn(PUTFIELD, mapping.getClassInternalName(), memberName, fieldDesc);
            }
        }
    }

    private static void generateProperty(final ObjectCreatorMethodVisitor ctor, final Property property) {
        if (property.isPrimitive()) {
            PrimitiveProperty primitiveProperty = property.asPrimitive();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            if (!primitiveProperty.hasConvertWith() && primitiveProperty.getPrimitiveType() == int.class) {
                ctor.visitMethod(PrimitiveMethodInvocation.intValue);
            } else if (!primitiveProperty.hasConvertWith() && primitiveProperty.getPrimitiveType() == boolean.class) {
                ctor.visitMethod(PrimitiveMethodInvocation.boolValue);
            } else {
                ctor.visitLdcInsn(Type.getType(primitiveProperty.getBoxType()));
                ctor.visitConverter(primitiveProperty);
                ctor.visitMethod(PrimitiveMethodInvocation.value);
            }
        } else if (property.isLeaf() && !property.isOptional()) {
            LeafProperty leafProperty = property.asLeaf();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            if (!leafProperty.hasConvertWith() && leafProperty.getValueType() == String.class) {
                ctor.visitMethod(ObjectMethodInvocation.stringValue);
            } else if (!leafProperty.hasConvertWith() && leafProperty.getValueRawType() == Integer.class) {
                ctor.visitMethod(ObjectMethodInvocation.integerValue);
            } else if (!leafProperty.hasConvertWith() && leafProperty.getValueRawType() == Boolean.class) {
                ctor.visitMethod(ObjectMethodInvocation.booleanValue);
            } else {
                ctor.visitLdcInsn(Type.getType(leafProperty.getValueRawType()));
                ctor.visitConverter(leafProperty);
                ctor.visitMethod(property.isSecret() ? ObjectMethodInvocation.secretValue : ObjectMethodInvocation.value);
            }
        } else if (property.isOptional() && property.isLeaf()) {
            LeafProperty optionalProperty = property.asLeaf();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            if (!optionalProperty.hasConvertWith() && !optionalProperty.isSecret()
                    && optionalProperty.getValueRawType() == String.class) {
                ctor.visitMethod(ObjectMethodInvocation.optionalStringValue);
            } else {
                ctor.visitLdcInsn(Type.getType(optionalProperty.getValueRawType()));
                ctor.visitConverter(optionalProperty);
                ctor.visitMethod(optionalProperty.isSecret() ? ObjectMethodInvocation.optionalSecretValue
                        : ObjectMethodInvocation.optionalValue);
            }
        } else if (property.isMap() && property.asMap().getValueProperty().isLeaf()) {
            MapProperty mapProperty = property.asMap();
            Property valueProperty = mapProperty.getValueProperty();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
            ctor.visitKeyConverter(mapProperty);
            ctor.visitInsn(valueProperty.isOptional() ? ICONST_1 : ICONST_0);
            ctor.visitLdcInsn(getType(valueProperty.asLeaf().getValueRawType()));
            ctor.visitConverter(valueProperty.asLeaf());
            ctor.visitKeyProvider(mapProperty);
            ctor.visitDefault(mapProperty);
            ctor.visitMethod(valueProperty.isSecret() ? MapMethodInvocation.secretValues : MapMethodInvocation.values);
        } else if (property.isMap() && property.asMap().getValueProperty().isCollection()
                && property.asMap().getValueProperty().asCollection().getElement().isLeaf()) {
            MapProperty mapProperty = property.asMap();
            Property valueProperty = mapProperty.getValueProperty();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
            ctor.visitKeyConverter(mapProperty);
            LeafProperty elementProperty = valueProperty.asCollection().getElement().asLeaf();
            ctor.visitLdcInsn(getType(elementProperty.getValueRawType()));
            ctor.visitConverter(elementProperty);
            ctor.visitLdcInsn(getType(valueProperty.asCollection().getCollectionRawType()));
            ctor.visitKeyProvider(mapProperty);
            ctor.visitDefault(mapProperty);
            ctor.visitMethod(
                    elementProperty.isSecret() ? MapCollectionInvocation.secretValues : MapCollectionInvocation.values);
        } else if (property.isCollection() && property.asCollection().getElement().isLeaf()) {
            CollectionProperty collectionProperty = property.asCollection();
            LeafProperty elementProperty = collectionProperty.getElement().asLeaf();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            ctor.visitLdcInsn(getType(elementProperty.getValueRawType()));
            ctor.visitConverter(elementProperty);
            ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
            ctor.visitMethod(
                    elementProperty.isSecret() ? CollectionMethodInvocation.secretValues : CollectionMethodInvocation.values);
        } else if (property.isOptional() && property.asOptional().getNestedProperty().isCollection()
                && property.asOptional().getNestedProperty().asCollection().getElement().isLeaf()) {
            CollectionProperty collectionProperty = property.asOptional().getNestedProperty().asCollection();
            LeafProperty elementProperty = collectionProperty.getElement().asLeaf();
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitPropertyName(property);
            ctor.visitLdcInsn(getType(elementProperty.getValueRawType()));
            ctor.visitConverter(elementProperty);
            ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
            ctor.visitMethod(elementProperty.isSecret() ? CollectionMethodInvocation.optionalSecretValues
                    : CollectionMethodInvocation.optionalValues);
        } else {
            ctor.visitTypeInsn(NEW, I_OBJECT_CREATOR);
            ctor.visitInsn(DUP);
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitLdcInsn(property.getPropertyName());
            ctor.visitInsn(property.hasPropertyName() ? ICONST_0 : ICONST_1);
            ctor.visitMethodInsn(INVOKESPECIAL, I_OBJECT_CREATOR, "<init>", "(" + D_MAPPING_CONTEXT + D_STRING + "Z" + ")V",
                    false);
            generateNestedProperty(ctor, property);
        }
    }

    private static void generateNestedProperty(final ObjectCreatorMethodVisitor ctor, final Property property) {
        if (property.isGroup()) {
            ctor.visitLdcInsn(getType(property.asGroup().getGroupType().getInterfaceType()));
            ctor.visitMethod(ObjectCreatorInvocation.group);
            ctor.visitMethod(ObjectCreatorInvocation.get);
        } else if (property.isMap() && property.asMap().getValueProperty().isLeaf()) {
            MapProperty mapProperty = property.asMap();
            Property valueProperty = mapProperty.getValueProperty();
            ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
            ctor.visitKeyConverter(mapProperty);
            LeafProperty leafProperty = valueProperty.asLeaf();
            ctor.visitLdcInsn(getType(leafProperty.getValueRawType()));
            ctor.visitConverter(leafProperty);
            ctor.visitKeyProvider(mapProperty);
            ctor.visitDefault(mapProperty);
            ctor.visitMethod(ObjectCreatorMapInvocation.values);
            ctor.visitMethod(ObjectCreatorInvocation.get);
        } else if (property.isMap() && property.asMap().getValueProperty().isCollection()
                && property.asMap().getValueProperty().asCollection().getElement().isLeaf()) {
            MapProperty mapProperty = property.asMap();
            Property valueProperty = mapProperty.getValueProperty();
            ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
            ctor.visitKeyConverter(mapProperty);
            LeafProperty leafProperty = valueProperty.asCollection().getElement().asLeaf();
            ctor.visitLdcInsn(getType(leafProperty.getValueRawType()));
            ctor.visitConverter(leafProperty);
            ctor.visitLdcInsn(getType(valueProperty.asCollection().getCollectionRawType()));
            ctor.visitKeyProvider(mapProperty);
            ctor.visitDefault(mapProperty);
            ctor.visitMethod(ObjectCreatorInvocation.values);
            ctor.visitMethod(ObjectCreatorInvocation.get);
        } else if (property.isOptional() && property.asOptional().getNestedProperty().isCollection()
                && property.asOptional().getNestedProperty().asCollection().getElement().isLeaf()) {
            CollectionProperty collectionProperty = property.asOptional().getNestedProperty().asCollection();
            ctor.visitLdcInsn(getType(collectionProperty.getElement().asLeaf().getValueRawType()));
            ctor.visitConverter(collectionProperty.getElement().asLeaf());
            ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
            ctor.visitMethod(ObjectCreatorInvocation.optionalValues);
            ctor.visitMethod(ObjectCreatorInvocation.get);
        } else if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            Property valueProperty = mapProperty.getValueProperty();
            if (valueProperty.isGroup()) {
                ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
                ctor.visitKeyConverter(mapProperty);
                ctor.visitKeyUnnamed(mapProperty);
                ctor.visitKeyProvider(mapProperty);
                if (mapProperty.hasDefaultValue()) {
                    ctor.visitLdcInsn(getType(valueProperty.asGroup().getGroupType().getInterfaceType()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                ctor.visitMethod(ObjectCreatorMapGroupInvocation.map);
                ctor.visitLdcInsn(getType(valueProperty.asGroup().getGroupType().getInterfaceType()));
                ctor.visitMethod(
                        mapProperty.hasKeyProvider() ? ObjectCreatorInvocation.group : ObjectCreatorInvocation.lazyGroup);
                ctor.visitMethod(ObjectCreatorInvocation.get);
            } else {
                unwrapNestedProperty(ctor, property);
            }
        } else if (property.isCollection()) {
            unwrapNestedProperty(ctor, property);
        } else if (property.isOptional()) {
            final MayBeOptionalProperty nestedProperty = property.asOptional().getNestedProperty();
            if (nestedProperty.isGroup()) {
                ctor.visitLdcInsn(getType(nestedProperty.asGroup().getGroupType().getInterfaceType()));
                ctor.visitMethod(ObjectCreatorInvocation.optionalGroup);
                ctor.visitMethod(ObjectCreatorInvocation.get);
            } else if (nestedProperty.isCollection()) {
                CollectionProperty collectionProperty = nestedProperty.asCollection();
                ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
                ctor.visitMethod(ObjectCreatorInvocation.optionalCollection);
                generateNestedProperty(ctor, collectionProperty.getElement());
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void unwrapNestedProperty(final ObjectCreatorMethodVisitor ctor, final Property property) {
        if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
            ctor.visitKeyConverter(mapProperty);
            ctor.visitKeyUnnamed(mapProperty);
            ctor.visitKeyProvider(mapProperty);
            ctor.visitMethod(ObjectCreatorInvocation.map);
            generateNestedProperty(ctor, mapProperty.getValueProperty());
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
            ctor.visitMethod(ObjectCreatorInvocation.collection);
            generateNestedProperty(ctor, collectionProperty.getElement());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static int getReturnInstruction(Property property) {
        PrimitiveProperty primitiveProperty;
        if (property.isPrimitive()) {
            primitiveProperty = property.asPrimitive();
        } else if (property.isDefaultMethod() && property.asDefaultMethod().getDefaultProperty().isPrimitive()) {
            primitiveProperty = property.asDefaultMethod().getDefaultProperty().asPrimitive();
        } else {
            return ARETURN;
        }

        if (primitiveProperty.getPrimitiveType() == float.class) {
            return FRETURN;
        } else if (primitiveProperty.getPrimitiveType() == double.class) {
            return DRETURN;
        } else if (primitiveProperty.getPrimitiveType() == long.class) {
            return LRETURN;
        } else {
            return IRETURN;
        }
    }

    private static String getSignature(final Field field) {
        final String typeName = field.getGenericType().getTypeName();
        if (typeName.indexOf('<') != -1 && typeName.indexOf('>') != -1) {
            String signature = "()L" + typeName.replace(".", "/");
            signature = signature.replace("<", "<L");
            signature = signature.replace(", ", ";L");
            signature = signature.replace(">", ";>");
            signature += ";";
            if (typeName.contains("[]")) {
                signature = ARRAY_FORMAT_REGEX.matcher(signature).replaceAll("$1[L$2;");
            }
            return signature;
        }

        return null;
    }

    private static void generateToString(final ClassVisitor visitor, final ConfigMappingInterface mapping) {
        if (!mapping.getToStringMethod().generate()) {
            return;
        }

        MethodVisitor ts = visitor.visitMethod(ACC_PUBLIC, "toString", "()L" + I_STRING + ";", null, null);
        ts.visitCode();
        ts.visitTypeInsn(NEW, I_STRING_BUILDER);
        ts.visitInsn(DUP);
        ts.visitMethodInsn(INVOKESPECIAL, I_STRING_BUILDER, "<init>", "()V", false);
        ts.visitLdcInsn(mapping.getInterfaceType().getSimpleName() + "{");
        ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(L" + I_STRING + ";)L" + I_STRING_BUILDER + ";", false);

        Property[] properties = mapping.getProperties();
        for (int i = 0, propertiesLength = properties.length; i < propertiesLength; i++) {
            Property property = properties[i];

            // Exclude Secrets from toString
            if (isSecret(property)) {
                continue;
            }

            if (property.isDefaultMethod()) {
                property = property.asDefaultMethod().getDefaultProperty();
            }

            String member = property.getMethod().getName();
            ts.visitLdcInsn(member + "=");
            ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(L" + I_STRING + ";)L" + I_STRING_BUILDER + ";",
                    false);
            ts.visitVarInsn(ALOAD, V_THIS);
            ts.visitFieldInsn(GETFIELD, mapping.getClassInternalName(), member,
                    getDescriptor(property.getMethod().getReturnType()));
            if (property.isPrimitive()) {
                ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append",
                        "(" + getDescriptor(property.asPrimitive().getPrimitiveType()) + ")L" + I_STRING_BUILDER + ";", false);
            } else {
                ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(L" + I_OBJECT + ";)L" + I_STRING_BUILDER + ";",
                        false);
            }

            if (i + 1 < propertiesLength) {
                ts.visitLdcInsn(", ");
                ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(L" + I_STRING + ";)L" + I_STRING_BUILDER + ";",
                        false);
            }
        }

        ts.visitLdcInsn("}");
        ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(L" + I_STRING + ";)L" + I_STRING_BUILDER + ";", false);
        ts.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "toString", "()L" + I_STRING + ";", false);

        ts.visitInsn(ARETURN);
        ts.visitEnd();
        ts.visitMaxs(0, 0);
    }

    private static void generateEquals(final ClassVisitor visitor, final ConfigMappingInterface mapping) {
        MethodVisitor eq = visitor.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        eq.visitCode();
        int V_O = 1;
        int V_THAT = 2;

        // if (this == o) {return true;}
        eq.visitVarInsn(ALOAD, V_THIS);
        eq.visitVarInsn(ALOAD, V_O);
        Label _ifRef = new Label();
        eq.visitJumpInsn(IF_ACMPNE, _ifRef);
        eq.visitInsn(ICONST_1);
        eq.visitInsn(IRETURN);
        eq.visitLabel(_ifRef);

        // if (o == null || getClass() != o.getClass()) {return false;}
        eq.visitVarInsn(ALOAD, V_O);
        Label _ifNull = new Label();
        eq.visitJumpInsn(IFNULL, _ifNull);
        eq.visitVarInsn(ALOAD, V_THIS);
        Label _ifClass = new Label();
        eq.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT, "getClass", "()L" + I_CLASS + ";", false);
        eq.visitVarInsn(ALOAD, V_O);
        eq.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT, "getClass", "()L" + I_CLASS + ";", false);
        eq.visitJumpInsn(IF_ACMPEQ, _ifClass);
        eq.visitLabel(_ifNull);
        eq.visitFrame(F_SAME, 0, null, 0, null);
        eq.visitInsn(ICONST_0);
        eq.visitInsn(IRETURN);
        eq.visitLabel(_ifClass);

        // ConfigMappingClass that = (ConfigMappingClass) o;
        eq.visitVarInsn(ALOAD, V_O);
        eq.visitTypeInsn(CHECKCAST, mapping.getClassInternalName());
        eq.visitVarInsn(ASTORE, V_THAT);

        // this.primitive() == that.primitive() && this.object().equals(that.object()) ...
        Label _ifTrue = new Label();
        Label _ifFalse = new Label();
        for (Property property : mapping.getProperties()) {
            // unwrap Kotlin default methods
            if (property.isDefaultMethod()) {
                property = property.asDefaultMethod().getDefaultProperty();
            }

            String member = property.getMethod().getName();
            Class<?> returnType = property.getMethod().getReturnType();

            eq.visitVarInsn(ALOAD, V_THIS);
            eq.visitMethodInsn(INVOKEVIRTUAL, mapping.getClassInternalName(), member, "()" + getDescriptor(returnType), false);
            eq.visitVarInsn(ALOAD, V_THAT);
            eq.visitMethodInsn(INVOKEVIRTUAL, mapping.getClassInternalName(), member, "()" + getDescriptor(returnType), false);
            if (property.isPrimitive()) {
                PrimitiveProperty primitiveProperty = property.asPrimitive();
                if (primitiveProperty.getPrimitiveType() == float.class) {
                    eq.visitInsn(FCMPL);
                    eq.visitJumpInsn(IFNE, _ifFalse);
                } else if (primitiveProperty.getPrimitiveType() == double.class) {
                    eq.visitInsn(DCMPL);
                    eq.visitJumpInsn(IFNE, _ifFalse);
                } else if (primitiveProperty.getPrimitiveType() == long.class) {
                    eq.visitInsn(LCMP);
                    eq.visitJumpInsn(IFNE, _ifFalse);
                } else {
                    eq.visitJumpInsn(IF_ICMPNE, _ifFalse);
                }
            } else {
                eq.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                        false);
                eq.visitJumpInsn(IFEQ, _ifFalse);
            }
        }

        // return
        eq.visitInsn(ICONST_1);
        eq.visitJumpInsn(GOTO, _ifTrue);
        eq.visitLabel(_ifFalse);
        eq.visitInsn(ICONST_0);
        eq.visitLabel(_ifTrue);
        eq.visitInsn(IRETURN);

        eq.visitEnd();
        eq.visitMaxs(0, 0);
    }

    private static void generateHashCode(final ClassVisitor visitor, final ConfigMappingInterface mapping) {
        MethodVisitor hc = visitor.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
        hc.visitCode();
        Property[] properties = mapping.getProperties();

        hc.visitIntInsn(BIPUSH, properties.length);
        hc.visitTypeInsn(ANEWARRAY, I_OBJECT);
        hc.visitInsn(DUP);

        for (int i = 0; i < properties.length; i++) {
            Property property = properties[i];
            // unwrap Kotlin default methods
            if (property.isDefaultMethod()) {
                property = property.asDefaultMethod().getDefaultProperty();
            }

            String member = property.getMethod().getName();
            Class<?> returnType = property.getMethod().getReturnType();

            hc.visitIntInsn(BIPUSH, i);
            hc.visitVarInsn(ALOAD, V_THIS);
            hc.visitFieldInsn(GETFIELD, mapping.getClassInternalName(), member, getDescriptor(returnType));
            if (property.isPrimitive()) {
                PrimitiveProperty primitiveProperty = property.asPrimitive();
                hc.visitMethodInsn(INVOKESTATIC, getInternalName(primitiveProperty.getBoxType()), "valueOf",
                        "(" + getDescriptor(primitiveProperty.getPrimitiveType()) + ")"
                                + getDescriptor(primitiveProperty.getBoxType()),
                        false);
            }
            hc.visitInsn(AASTORE);
            hc.visitInsn(DUP);
        }

        hc.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hash", "([Ljava/lang/Object;)I", false);
        hc.visitInsn(IRETURN);

        hc.visitMaxs(0, 0);
        hc.visitEnd();
    }

    private static void generateStaticInit(final ClassVisitor classVisitor, final ConfigMappingInterface mapping) {
        Map<String, Property> properties = ConfigMappingInterface.getProperties(mapping).get(mapping.getInterfaceType())
                .get("");

        classVisitor.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "PROPERTIES", "Ljava/util/Map;",
                "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", null).visitEnd();
        classVisitor.visitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "SECRETS", "Ljava/util/Set;",
                "Ljava/util/Set<Ljava/lang/String;>;", null).visitEnd();

        MethodVisitor clinit = classVisitor.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);

        // PROPERTIES
        clinit.visitTypeInsn(NEW, "java/util/HashMap");
        clinit.visitInsn(DUP);
        if (properties.size() < 3) {
            clinit.visitIntInsn(BIPUSH, properties.size() + 1);
        } else {
            clinit.visitIntInsn(SIPUSH, (int) ((float) properties.size() / 0.75f + 1.0f));
        }
        clinit.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "(I)V", false);
        clinit.visitFieldInsn(PUTSTATIC, mapping.getClassInternalName(), "PROPERTIES", "Ljava/util/Map;");

        // SECRETS
        Map<String, Property> secrets = new HashMap<>();
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            if (isSecret(entry.getValue())) {
                secrets.put(entry.getKey(), entry.getValue());
            }
        }

        if (secrets.isEmpty()) {
            clinit.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptySet", "()Ljava/util/Set;", false);
        } else if (secrets.size() < 3) {
            clinit.visitTypeInsn(NEW, "java/util/HashSet");
            clinit.visitInsn(DUP);
            clinit.visitIntInsn(BIPUSH, secrets.size() + 1);
            clinit.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "(I)V", false);
        } else {
            clinit.visitTypeInsn(NEW, "java/util/HashSet");
            clinit.visitInsn(DUP);
            clinit.visitIntInsn(SIPUSH, (int) ((float) secrets.size() / 0.75f + 1.0f));
            clinit.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "(I)V", false);
        }
        clinit.visitFieldInsn(PUTSTATIC, mapping.getClassInternalName(), "SECRETS", "Ljava/util/Set;");

        // PROPERTIES
        for (Map.Entry<String, Property> entry : properties.entrySet()) {
            clinit.visitFieldInsn(GETSTATIC, mapping.getClassInternalName(), "PROPERTIES", "Ljava/util/Map;");
            clinit.visitLdcInsn(entry.getKey());
            if (entry.getValue().hasDefaultValue()) {
                // Defaults for collections also come as a simple property with comma separated values, no need for the star name
                if (entry.getKey().endsWith("[*]")) {
                    clinit.visitInsn(ACONST_NULL);
                } else {
                    clinit.visitLdcInsn(entry.getValue().getDefaultValue());
                }
            } else {
                clinit.visitInsn(ACONST_NULL);
            }
            clinit.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            clinit.visitInsn(POP);
        }

        // SECRETS
        for (Entry<String, Property> entry : secrets.entrySet()) {
            clinit.visitFieldInsn(GETSTATIC, mapping.getClassInternalName(), "SECRETS", "Ljava/util/Set;");
            clinit.visitLdcInsn(entry.getKey());
            clinit.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
            clinit.visitInsn(POP);
        }

        clinit.visitInsn(RETURN);
        clinit.visitMaxs(0, 0);
        clinit.visitEnd();

        MethodVisitor mv;
        mv = classVisitor.visitMethod(ACC_PUBLIC | ACC_STATIC, "getProperties", "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;", null);
        mv.visitFieldInsn(GETSTATIC, mapping.getClassInternalName(), "PROPERTIES", "Ljava/util/Map;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = classVisitor.visitMethod(ACC_PUBLIC | ACC_STATIC, "getSecrets", "()Ljava/util/Set;",
                "()Ljava/util/Set<Ljava/lang/String;>;",
                null);
        mv.visitFieldInsn(GETSTATIC, mapping.getClassInternalName(), "SECRETS", "Ljava/util/Set;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static boolean hasDefaultValue(final Class<?> klass, final Object value) {
        if (value == null) {
            return false;
        }

        if (klass.isPrimitive() && value instanceof Number && value.equals(0)) {
            return false;
        }

        if (klass.isPrimitive() && value instanceof Boolean && value.equals(Boolean.FALSE)) {
            return false;
        }

        return !klass.isPrimitive() || !(value instanceof Character) || !value.equals(0);
    }

    private static boolean isSecret(final Property property) {
        if (property.isLeaf()) {
            return property.isSecret();
        } else if (property.isOptional()) {
            return isSecret(property.asOptional().getNestedProperty());
        } else if (property.isCollection()) {
            return isSecret(property.asCollection().getElement());
        } else if (property.isMap()) {
            return isSecret(property.asMap().getValueProperty());
        }
        return false;
    }

    private static class ObjectCreatorMethodVisitor extends MethodVisitor {
        protected ObjectCreatorMethodVisitor(MethodVisitor methodVisitor) {
            super(Opcodes.ASM9, methodVisitor);
        }

        void visitCast(Property property, String fieldType) {
            if (property.isPrimitive()) {
                PrimitiveProperty primitive = property.asPrimitive();
                if (!property.hasConvertWith() && primitive.getPrimitiveType() == int.class) {
                    // No cast needed!
                } else if (!property.hasConvertWith() && primitive.getPrimitiveType() == boolean.class) {
                    // No cast needed!
                } else {
                    this.visitTypeInsn(CHECKCAST, getInternalName(primitive.getBoxType()));
                    this.visitMethodInsn(INVOKEVIRTUAL, getInternalName(primitive.getBoxType()), primitive.getUnboxMethodName(),
                            primitive.getUnboxMethodDescriptor(), false);
                }
            } else {
                this.visitTypeInsn(CHECKCAST, fieldType);
            }
        }

        void visitPropertyName(Property property) {
            this.visitInsn(property.hasPropertyName() ? ICONST_0 : ICONST_1);
            this.visitLdcInsn(property.getPropertyName());
        }

        void visitConverter(PrimitiveProperty property) {
            if (property.hasConvertWith()) {
                this.visitLdcInsn(getType(property.getConvertWith()));
            } else {
                this.visitInsn(ACONST_NULL);
            }
        }

        void visitConverter(LeafProperty property) {
            if (property.hasConvertWith()) {
                this.visitLdcInsn(getType(property.getConvertWith()));
            } else {
                this.visitInsn(ACONST_NULL);
            }
        }

        void visitKeyConverter(MapProperty property) {
            if (property.hasKeyConvertWith()) {
                this.visitLdcInsn(getType(property.getKeyConvertWith()));
            } else {
                this.visitInsn(ACONST_NULL);
            }
        }

        void visitKeyUnnamed(MapProperty property) {
            if (property.hasKeyUnnamed()) {
                this.visitLdcInsn(property.getKeyUnnamed());
            } else {
                this.visitInsn(ACONST_NULL);
            }
        }

        void visitKeyProvider(MapProperty property) {
            if (property.hasKeyProvider()) {
                String provider = getInternalName(property.getKeysProvider());
                visitTypeInsn(NEW, provider);
                visitInsn(DUP);
                visitMethodInsn(INVOKESPECIAL, provider, "<init>", "()V", false);
                visitMethodInsn(INVOKEVIRTUAL, provider, "get", "()L" + I_ITERABLE + ";", false);
            } else {
                this.visitInsn(ACONST_NULL);
            }
        }

        void visitDefault(MapProperty property) {
            if (property.hasDefaultValue() && property.getDefaultValue() != null) {
                this.visitLdcInsn(property.getDefaultValue());
            } else {
                this.visitInsn(ACONST_NULL);
            }
        }

        void visitMethod(MethodInvocation methodInvocation) {
            methodInvocation.invoke(this);
        }
    }

    private interface MethodInvocation {
        default void invoke(final MethodVisitor mv) {
            mv.visitMethodInsn(opcode(), I_OBJECT_CREATOR, name(), desc(), false);
        }

        int opcode();

        String name();

        String desc();
    }

    private static final String D_MAPPING_CONTEXT = getDescriptor(ConfigMappingContext.class);
    private static final String D_OBJECT_CREATOR = getDescriptor(ObjectCreator.class);
    private static final String D_CLASS = getDescriptor(Class.class);
    private static final String D_OBJECT = getDescriptor(Object.class);
    private static final String D_STRING = getDescriptor(String.class);
    private static final String D_INTEGER = getDescriptor(Integer.class);
    private static final String D_BOOLEAN = getDescriptor(Boolean.class);
    private static final String D_OPTIONAL = getDescriptor(Optional.class);
    private static final String D_MAP = getDescriptor(Map.class);
    private static final String D_COLLECTION = getDescriptor(Collection.class);
    private static final String D_ITERABLE = getDescriptor(Iterable.class);
    private static final String D_SECRET = getDescriptor(Secret.class);

    private enum PrimitiveMethodInvocation implements MethodInvocation {
        value(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + ")" + D_OBJECT),
        intValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + ")" + "I"),
        boolValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + ")" + "Z"),
        ;

        private final int opcode;
        private final String desc;

        PrimitiveMethodInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum ObjectMethodInvocation implements MethodInvocation {
        value(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + ")" + D_OBJECT),
        secretValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + ")" + D_SECRET),
        optionalValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + ")" + D_OPTIONAL),
        optionalSecretValue(INVOKESTATIC, optionalValue.desc),
        stringValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + ")" + D_STRING),
        integerValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + ")" + D_INTEGER),
        booleanValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + ")" + D_BOOLEAN),
        optionalStringValue(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + ")" + D_OPTIONAL),
        ;

        private final int opcode;
        private final String desc;

        ObjectMethodInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum MapMethodInvocation implements MethodInvocation {
        values(INVOKESTATIC,
                "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + "Z" + D_CLASS + D_CLASS + D_ITERABLE + D_STRING
                        + ")"
                        + D_MAP),
        secretValues(INVOKESTATIC, values.desc),
        ;

        private final int opcode;
        private final String desc;

        MapMethodInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum CollectionMethodInvocation implements MethodInvocation {
        values(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + D_CLASS + ")" + D_COLLECTION),
        secretValues(INVOKESTATIC, values.desc),
        optionalValues(INVOKESTATIC, "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + D_CLASS + ")" + D_OPTIONAL),
        optionalSecretValues(INVOKESTATIC, optionalValues.desc),
        ;

        private final int opcode;
        private final String desc;

        CollectionMethodInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum MapCollectionInvocation implements MethodInvocation {
        values(INVOKESTATIC,
                "(" + D_MAPPING_CONTEXT + "Z" + D_STRING + D_CLASS + D_CLASS + D_CLASS + D_CLASS + D_CLASS + D_ITERABLE
                        + D_STRING + ")" + D_MAP),
        secretValues(INVOKESTATIC, values.desc),
        ;

        private final int opcode;
        private final String desc;

        MapCollectionInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum ObjectCreatorInvocation implements MethodInvocation {
        get(INVOKEVIRTUAL, "()" + D_OBJECT),
        group(INVOKEVIRTUAL, "(" + D_CLASS + ")" + D_OBJECT_CREATOR),
        lazyGroup(INVOKEVIRTUAL, group.desc),
        optionalGroup(INVOKEVIRTUAL, group.desc),
        collection(INVOKEVIRTUAL, "(" + D_CLASS + ")" + D_OBJECT_CREATOR),
        optionalCollection(INVOKEVIRTUAL, collection.desc),
        map(INVOKEVIRTUAL, "(" + D_CLASS + D_CLASS + D_STRING + D_ITERABLE + ")" + D_OBJECT_CREATOR),
        values(INVOKEVIRTUAL,
                "(" + D_CLASS + D_CLASS + D_CLASS + D_CLASS + D_CLASS + D_ITERABLE + D_STRING + ")" + D_OBJECT_CREATOR),
        optionalValues(INVOKEVIRTUAL, "(" + D_CLASS + D_CLASS + D_CLASS + ")" + D_OBJECT_CREATOR),
        ;

        private final int opcode;
        private final String desc;

        ObjectCreatorInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum ObjectCreatorMapInvocation implements MethodInvocation {
        values(INVOKEVIRTUAL, "(" + D_CLASS + D_CLASS + D_CLASS + D_CLASS + D_ITERABLE + D_STRING + ")" + D_OBJECT_CREATOR),
        ;

        private final int opcode;
        private final String desc;

        ObjectCreatorMapInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
        }
    }

    private enum ObjectCreatorMapGroupInvocation implements MethodInvocation {
        map(INVOKEVIRTUAL, "(" + D_CLASS + D_CLASS + D_STRING + D_ITERABLE + D_CLASS + ")" + D_OBJECT_CREATOR);

        private final int opcode;
        private final String desc;

        ObjectCreatorMapGroupInvocation(int opcode, String desc) {
            this.opcode = opcode;
            this.desc = desc;
        }

        @Override
        public int opcode() {
            return opcode;
        }

        @Override
        public String desc() {
            return desc;
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
                super(ASM7, cw);
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
