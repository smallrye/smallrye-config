package io.smallrye.config;

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
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.I2C;
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
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.smallrye.config.ConfigMapping.NamingStrategy;
import io.smallrye.config.ConfigMappingInterface.CollectionProperty;
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
    private static final String I_CONFIGURATION_OBJECT = getInternalName(ConfigMappingObject.class);
    private static final String I_MAPPING_CONTEXT = getInternalName(ConfigMappingContext.class);
    private static final String I_NAMING_STRATEGY = getInternalName(NamingStrategy.class);
    private static final String I_OBJECT_CREATOR = getInternalName(ConfigMappingContext.ObjectCreator.class);
    private static final String I_OBJECT = getInternalName(Object.class);
    private static final String I_RUNTIME_EXCEPTION = getInternalName(RuntimeException.class);
    private static final String I_STRING_BUILDER = getInternalName(StringBuilder.class);
    private static final String I_STRING = getInternalName(String.class);
    private static final String I_ITERABLE = getInternalName(Iterable.class);

    private static final int V_THIS = 0;
    private static final int V_MAPPING_CONTEXT = 1;
    private static final int V_STRING_BUILDER = 2;
    private static final int V_LENGTH = 3;
    private static final int V_NAMING_STRATEGY = 4;

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
                new String[] {
                        I_CONFIGURATION_OBJECT,
                        getInternalName(mapping.getInterfaceType())
                });
        visitor.visitSource(null, null);

        // No Args Constructor - To use for proxies
        MethodVisitor noArgsCtor = visitor.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        noArgsCtor.visitVarInsn(ALOAD, V_THIS);
        noArgsCtor.visitMethodInsn(INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);
        noArgsCtor.visitInsn(RETURN);
        noArgsCtor.visitEnd();
        noArgsCtor.visitMaxs(0, 0);

        MethodVisitor ctor = visitor.visitMethod(ACC_PUBLIC, "<init>", "(L" + I_MAPPING_CONTEXT + ";)V", null, null);
        ctor.visitParameter("context", ACC_FINAL);
        Label ctorStart = new Label();
        Label ctorEnd = new Label();
        ctor.visitLabel(ctorStart);
        // stack: -
        ctor.visitVarInsn(ALOAD, V_THIS);
        // stack: this
        ctor.visitMethodInsn(INVOKESPECIAL, I_OBJECT, "<init>", "()V", false);
        // stack: -
        ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
        // stack: ctxt
        ctor.visitMethodInsn(INVOKEVIRTUAL, I_MAPPING_CONTEXT, "getNameBuilder", "()L" + I_STRING_BUILDER + ';',
                false);
        // stack: sb
        ctor.visitInsn(DUP);
        // stack: sb sb
        Label ctorSbStart = new Label();
        ctor.visitLabel(ctorSbStart);
        ctor.visitVarInsn(ASTORE, V_STRING_BUILDER);
        // stack: sb
        ctor.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "length", "()I", false);
        // stack: len
        Label ctorLenStart = new Label();
        ctor.visitLabel(ctorLenStart);
        ctor.visitVarInsn(ISTORE, V_LENGTH);

        Label ctorNsStart = new Label();
        ctor.visitLabel(ctorNsStart);
        ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);

        if (mapping.hasNamingStrategy()) {
            ctor.visitFieldInsn(GETSTATIC, I_NAMING_STRATEGY, mapping.getNamingStrategy().name(),
                    "L" + I_NAMING_STRATEGY + ";");
        } else {
            ctor.visitInsn(ACONST_NULL);
        }
        ctor.visitMethodInsn(INVOKEVIRTUAL, I_MAPPING_CONTEXT, "applyNamingStrategy",
                "(L" + I_NAMING_STRATEGY + ";)L" + I_NAMING_STRATEGY + ";", false);
        ctor.visitVarInsn(ASTORE, V_NAMING_STRATEGY);

        addProperties(visitor, ctor, new HashSet<>(), mapping, mapping.getClassInternalName());

        ctor.visitInsn(RETURN);
        ctor.visitLabel(ctorEnd);
        ctor.visitLocalVariable("mc", 'L' + I_MAPPING_CONTEXT + ';', null, ctorStart, ctorEnd, V_MAPPING_CONTEXT);
        ctor.visitLocalVariable("sb", 'L' + I_STRING_BUILDER + ';', null, ctorSbStart, ctorEnd, V_STRING_BUILDER);
        ctor.visitLocalVariable("len", "I", null, ctorLenStart, ctorEnd, V_LENGTH);
        ctor.visitLocalVariable("ns", "Lio/smallrye/config/ConfigMapping$NamingStrategy;", null, ctorNsStart, ctorEnd,
                V_NAMING_STRATEGY);
        ctor.visitEnd();
        ctor.visitMaxs(0, 0);
        visitor.visitEnd();

        generateEquals(visitor, mapping);
        generateHashCode(visitor, mapping);
        generateToString(visitor, mapping);
        generateNames(visitor, mapping);
        generateDefaults(visitor, mapping);

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
     * The generated configuration interface implements {@link ConfigMappingClassMapper} which provides the brige
     * between the instace of the configuration class and the implementation of the configuration interface provided by
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
            final MethodVisitor ctor,
            final Set<String> visited,
            final ConfigMappingInterface mapping,
            final String className) {

        for (Property property : mapping.getProperties()) {
            Method method = property.getMethod();
            String memberName = method.getName();

            // skip super members with overrides
            if (!visited.add(memberName)) {
                continue;
            }

            // Field Declaration
            String fieldType = getInternalName(method.getReturnType());
            String fieldDesc = getDescriptor(method.getReturnType());
            cv.visitField(ACC_PRIVATE, memberName, fieldDesc, null, null);

            // Getter
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, memberName, "()" + fieldDesc, null, null);
            mv.visitVarInsn(ALOAD, V_THIS);
            mv.visitFieldInsn(GETFIELD, className, memberName, fieldDesc);
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

            appendPropertyName(ctor, property);
            ctor.visitVarInsn(ALOAD, V_THIS);
            ctor.visitTypeInsn(NEW, I_OBJECT_CREATOR);
            ctor.visitInsn(DUP);
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitVarInsn(ALOAD, V_STRING_BUILDER);
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "toString", "()L" + I_STRING + ';', false);
            ctor.visitMethodInsn(INVOKESPECIAL, I_OBJECT_CREATOR, "<init>", "(L" + I_MAPPING_CONTEXT + ";L" + I_STRING + ";)V",
                    false);

            // try
            ctor.visitLabel(_try);

            generateProperty(ctor, property);

            ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "get", "()L" + I_OBJECT + ";", false);
            if (property.isPrimitive()) {
                PrimitiveProperty primitive = property.asPrimitive();
                ctor.visitTypeInsn(CHECKCAST, getInternalName(primitive.getBoxType()));
                ctor.visitMethodInsn(INVOKEVIRTUAL, getInternalName(primitive.getBoxType()), primitive.getUnboxMethodName(),
                        primitive.getUnboxMethodDescriptor(), false);
            } else {
                ctor.visitTypeInsn(CHECKCAST, fieldType);
            }
            ctor.visitFieldInsn(PUTFIELD, className, memberName, fieldDesc);
            ctor.visitJumpInsn(GOTO, _continue);

            // catch
            ctor.visitLabel(_catch);
            ctor.visitVarInsn(ALOAD, V_MAPPING_CONTEXT);
            ctor.visitInsn(SWAP);
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_MAPPING_CONTEXT, "reportProblem", "(L" + I_RUNTIME_EXCEPTION + ";)V", false);
            ctor.visitJumpInsn(GOTO, _continue);

            ctor.visitLabel(_continue);

            restoreLength(ctor);
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
                ctor.visitFieldInsn(PUTFIELD, className, memberName, fieldDesc);
            }
        }
    }

    private static void generateProperty(final MethodVisitor ctor, final Property property) {
        if (property.isLeaf() || property.isPrimitive() || property.isLeaf() && property.isOptional()) {
            Class<?> rawType = property.isLeaf() ? property.asLeaf().getValueRawType() : property.asPrimitive().getBoxType();
            ctor.visitLdcInsn(Type.getType(rawType));
            if (property.hasConvertWith() || property.isLeaf() && property.asLeaf().hasConvertWith()) {
                ctor.visitLdcInsn(getType(
                        property.isLeaf() ? property.asLeaf().getConvertWith() : property.asPrimitive().getConvertWith()));
            } else {
                ctor.visitInsn(ACONST_NULL);
            }
            if (property.isOptional()) {
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "optionalValue",
                        "(L" + I_CLASS + ";L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
            } else {
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "value",
                        "(L" + I_CLASS + ";L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
            }
        } else if (property.isGroup()) {
            ctor.visitLdcInsn(getType(property.asGroup().getGroupType().getInterfaceType()));
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "group", "(L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";",
                    false);
        } else if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            Property valueProperty = mapProperty.getValueProperty();
            if (valueProperty.isLeaf()) {
                ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
                if (mapProperty.hasKeyConvertWith()) {
                    ctor.visitLdcInsn(getType(mapProperty.getKeyConvertWith()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                LeafProperty leafProperty = valueProperty.asLeaf();
                ctor.visitLdcInsn(getType(leafProperty.getValueRawType()));
                if (leafProperty.hasConvertWith()) {
                    ctor.visitLdcInsn(getType(leafProperty.getConvertWith()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                if (mapProperty.hasKeyProvider()) {
                    generateMapKeysProvider(ctor, mapProperty.getKeysProvider());
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                if (mapProperty.hasDefaultValue() && mapProperty.getDefaultValue() != null) {
                    ctor.visitLdcInsn(mapProperty.getDefaultValue());
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "values", "(L" + I_CLASS + ";L" + I_CLASS + ";L" + I_CLASS
                        + ";L" + I_CLASS + ";L" + I_ITERABLE + ";L" + I_STRING + ";)L" + I_OBJECT_CREATOR + ";", false);
            } else if (valueProperty.isGroup()) {
                ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
                if (mapProperty.hasKeyConvertWith()) {
                    ctor.visitLdcInsn(getType(mapProperty.getKeyConvertWith()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                if (mapProperty.hasKeyUnnamed()) {
                    ctor.visitLdcInsn(mapProperty.getKeyUnnamed());
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                if (mapProperty.hasKeyProvider()) {
                    generateMapKeysProvider(ctor, mapProperty.getKeysProvider());
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                if (mapProperty.hasDefaultValue()) {
                    ctor.visitLdcInsn(getType(valueProperty.asGroup().getGroupType().getInterfaceType()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "map",
                        "(L" + I_CLASS + ";L" + I_CLASS + ";L" + I_STRING + ";L" + I_ITERABLE + ";L" + I_CLASS + ";)L"
                                + I_OBJECT_CREATOR + ";",
                        false);
                ctor.visitLdcInsn(getType(valueProperty.asGroup().getGroupType().getInterfaceType()));
                if (mapProperty.hasKeyProvider()) {
                    ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "group",
                            "(L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
                } else {
                    ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "lazyGroup",
                            "(L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
                }
            } else if (valueProperty.isCollection() && valueProperty.asCollection().getElement().isLeaf()) {
                ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
                if (mapProperty.hasKeyConvertWith()) {
                    ctor.visitLdcInsn(getType(mapProperty.getKeyConvertWith()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                LeafProperty leafProperty = mapProperty.getValueProperty().asCollection().getElement().asLeaf();
                ctor.visitLdcInsn(getType(leafProperty.getValueRawType()));
                if (leafProperty.hasConvertWith()) {
                    ctor.visitLdcInsn(getType(leafProperty.getConvertWith()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                ctor.visitLdcInsn(getType(mapProperty.getValueProperty().asCollection().getCollectionRawType()));
                if (mapProperty.hasKeyProvider()) {
                    generateMapKeysProvider(ctor, mapProperty.getKeysProvider());
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                if (mapProperty.hasDefaultValue()) {
                    ctor.visitLdcInsn(mapProperty.getDefaultValue());
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                ctor.visitMethodInsn(
                        INVOKEVIRTUAL, I_OBJECT_CREATOR, "values", "(L" + I_CLASS + ";L" + I_CLASS + ";L" + I_CLASS + ";L"
                                + I_CLASS + ";L" + I_CLASS + ";L" + I_ITERABLE + ";L" + I_STRING + ";)L" + I_OBJECT_CREATOR
                                + ";",
                        false);
            } else {
                unwrapProperty(ctor, property);
            }
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            if (collectionProperty.getElement().isLeaf()) {
                ctor.visitLdcInsn(getType(collectionProperty.getElement().asLeaf().getValueRawType()));
                if (collectionProperty.getElement().hasConvertWith()) {
                    ctor.visitLdcInsn(getType(collectionProperty.getElement().asLeaf().getConvertWith()));
                } else {
                    ctor.visitInsn(ACONST_NULL);
                }
                ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "values",
                        "(L" + I_CLASS + ";L" + I_CLASS + ";L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
            } else {
                unwrapProperty(ctor, property);
            }
        } else if (property.isOptional()) {
            final MayBeOptionalProperty nestedProperty = property.asOptional().getNestedProperty();
            if (nestedProperty.isGroup()) {
                ctor.visitLdcInsn(getType(nestedProperty.asGroup().getGroupType().getInterfaceType()));
                ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "optionalGroup",
                        "(L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
            } else if (nestedProperty.isCollection()) {
                CollectionProperty collectionProperty = nestedProperty.asCollection();
                if (collectionProperty.getElement().isLeaf()) {
                    ctor.visitLdcInsn(getType(collectionProperty.getElement().asLeaf().getValueRawType()));
                    if (collectionProperty.getElement().hasConvertWith()) {
                        ctor.visitLdcInsn(getType(collectionProperty.getElement().asLeaf().getConvertWith()));
                    } else {
                        ctor.visitInsn(ACONST_NULL);
                    }
                    ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
                    ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "optionalValues",
                            "(L" + I_CLASS + ";L" + I_CLASS + ";L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
                } else {
                    ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
                    ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "optionalCollection",
                            "(L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";", false);
                    generateProperty(ctor, collectionProperty.getElement());
                }
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void unwrapProperty(final MethodVisitor ctor, final Property property) {
        if (property.isMap()) {
            MapProperty mapProperty = property.asMap();
            ctor.visitLdcInsn(getType(mapProperty.getKeyRawType()));
            if (mapProperty.hasKeyConvertWith()) {
                ctor.visitLdcInsn(getType(mapProperty.getKeyConvertWith()));
            } else {
                ctor.visitInsn(ACONST_NULL);
            }
            if (mapProperty.hasKeyUnnamed()) {
                ctor.visitLdcInsn(mapProperty.getKeyUnnamed());
            } else {
                ctor.visitInsn(ACONST_NULL);
            }
            if (mapProperty.hasKeyProvider()) {
                generateMapKeysProvider(ctor, mapProperty.getKeysProvider());
            } else {
                ctor.visitInsn(ACONST_NULL);
            }
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "map",
                    "(L" + I_CLASS + ";L" + I_CLASS + ";L" + I_STRING + ";L" + I_ITERABLE + ";)L" + I_OBJECT_CREATOR + ";",
                    false);
            generateProperty(ctor, mapProperty.getValueProperty());
        } else if (property.isCollection()) {
            CollectionProperty collectionProperty = property.asCollection();
            ctor.visitLdcInsn(getType(collectionProperty.getCollectionRawType()));
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_OBJECT_CREATOR, "collection", "(L" + I_CLASS + ";)L" + I_OBJECT_CREATOR + ";",
                    false);
            generateProperty(ctor, collectionProperty.getElement());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void generateMapKeysProvider(final MethodVisitor ctor,
            final Class<? extends Supplier<Iterable<String>>> mapKeysProvider) {
        String provider = getInternalName(mapKeysProvider);
        ctor.visitTypeInsn(NEW, provider);
        ctor.visitInsn(DUP);
        ctor.visitMethodInsn(INVOKESPECIAL, provider, "<init>", "()V", false);
        ctor.visitMethodInsn(INVOKEVIRTUAL, provider, "get", "()L" + I_ITERABLE + ";", false);
    }

    private static void appendPropertyName(final MethodVisitor ctor, final Property property) {
        if (property.isParentPropertyName()) {
            return;
        }

        Label _continue = new Label();
        ctor.visitVarInsn(ALOAD, V_STRING_BUILDER);
        ctor.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "length", "()I", false);
        ctor.visitJumpInsn(IFEQ, _continue);

        ctor.visitVarInsn(ALOAD, V_STRING_BUILDER);
        ctor.visitLdcInsn('.');
        ctor.visitInsn(I2C);
        ctor.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(C)L" + I_STRING_BUILDER + ';', false);
        ctor.visitInsn(POP);
        ctor.visitLabel(_continue);

        ctor.visitVarInsn(ALOAD, V_STRING_BUILDER);
        if (property.hasPropertyName()) {
            ctor.visitLdcInsn(property.getPropertyName());
        } else {
            ctor.visitVarInsn(ALOAD, V_NAMING_STRATEGY);
            ctor.visitLdcInsn(property.getPropertyName());
            ctor.visitMethodInsn(INVOKEVIRTUAL, I_NAMING_STRATEGY, "apply", "(L" + I_STRING + ";)L" + I_STRING + ";", false);
        }

        ctor.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "append", "(L" + I_STRING + ";)L" + I_STRING_BUILDER + ';',
                false);
        ctor.visitInsn(POP);
    }

    private static void restoreLength(final MethodVisitor ctor) {
        ctor.visitVarInsn(ALOAD, V_STRING_BUILDER);
        ctor.visitVarInsn(ILOAD, V_LENGTH);
        ctor.visitMethodInsn(INVOKEVIRTUAL, I_STRING_BUILDER, "setLength", "(I)V", false);
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

    private static void generateNames(final ClassVisitor classVisitor, final ConfigMappingInterface mapping) {
        MethodVisitor mv = classVisitor.visitMethod(ACC_PUBLIC | ACC_STATIC, "getNames", "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/util/Set<Ljava/lang/String;>;>;>;",
                null);

        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 0);

        for (Map.Entry<String, Map<String, Set<String>>> mappings : mapping.getNames().entrySet()) {
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, 1);
            for (Map.Entry<String, Set<String>> paths : mappings.getValue().entrySet()) {
                mv.visitTypeInsn(NEW, "java/util/HashSet");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false);
                mv.visitVarInsn(ASTORE, 2);
                for (String name : paths.getValue()) {
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitLdcInsn(name);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
                    mv.visitInsn(POP);
                }
                mv.visitVarInsn(ALOAD, 1);
                mv.visitLdcInsn(paths.getKey());
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitInsn(POP);
            }
            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(mappings.getKey());
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void generateDefaults(final ClassVisitor classVisitor, final ConfigMappingInterface mapping) {
        MethodVisitor mv = classVisitor.visitMethod(ACC_PUBLIC | ACC_STATIC, "getDefaults", "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                null);

        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 0);

        for (Map.Entry<String, Property> entry : ConfigMappingInterface.getProperties(mapping)
                .get(mapping.getInterfaceType())
                .get("").entrySet()) {
            if (entry.getValue().hasDefaultValue()) {
                // Defaults for collections also come as a simple property with comma separated values, no need for the star name
                if (entry.getKey().endsWith("[*]")) {
                    continue;
                }

                mv.visitVarInsn(ALOAD, 0);
                mv.visitLdcInsn(entry.getKey());
                mv.visitLdcInsn(entry.getValue().getDefaultValue());
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitInsn(POP);
            }
        }

        mv.visitVarInsn(ALOAD, 0);
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
