package io.smallrye.config;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodDescriptor;
import static org.objectweb.asm.Type.getType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.smallrye.common.classloader.ClassDefiner;

final class ConfigMappingClass {
    private static final ClassValue<Class<?>> cv = new ClassValue<Class<?>>() {
        @Override
        protected Class<?> computeValue(final Class<?> type) {
            return createConfigurationClass(type);
        }
    };

    private static final String I_OBJECT = getInternalName(Object.class);
    private static final String I_CLASS = getInternalName(Class.class);
    private static final String I_FIELD = getInternalName(Field.class);

    static Class<?> toInterface(final Class<?> classType) {
        return cv.get(classType);
    }

    private static Class<?> createConfigurationClass(final Class<?> classType) {
        if (classType.isInterface() && classType.getTypeParameters().length == 0 ||
                Modifier.isAbstract(classType.getModifiers()) ||
                classType.isEnum()) {
            return classType;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        String classInternalName = getInternalName(classType);
        String interfaceName = classType.getName() + "I";
        String interfaceInternalName = interfaceName.replace('.', '/');

        writer.visit(V1_8, ACC_PUBLIC | ACC_INTERFACE | ACC_ABSTRACT, interfaceInternalName, null, I_OBJECT,
                new String[] { getInternalName(ConfigMappingClassMapper.class) });

        Field[] declaredFields = classType.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            writer.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, declaredField.getName(),
                    getMethodDescriptor(getType(declaredField.getType())), getSignature(declaredField), null);
            writer.visitEnd();
        }

        MethodVisitor ctor = writer.visitMethod(ACC_PUBLIC, "map", "()L" + I_OBJECT + ";", null, null);
        Label ctorStart = new Label();
        ctor.visitLabel(ctorStart);
        ctor.visitTypeInsn(NEW, classInternalName);
        ctor.visitInsn(DUP);
        ctor.visitMethodInsn(INVOKESPECIAL, classInternalName, "<init>", "()V", false);
        ctor.visitVarInsn(ASTORE, 1);

        for (Field declaredField : declaredFields) {
            String name = declaredField.getName();
            Class<?> type = declaredField.getType();

            if (declaredField.isAccessible()) {
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

        return ClassDefiner.defineClass(MethodHandles.lookup(), ConfigMappingClass.class, interfaceName,
                writer.toByteArray());
    }

    private static String getSignature(final Field field) {
        final String typeName = field.getGenericType().getTypeName();
        if (typeName.indexOf('<') != -1 && typeName.indexOf('>') != -1) {
            String signature = "()L" + typeName.replace(".", "/");
            signature = signature.replace("<", "<L");
            signature = signature.replace(">", ";>;");
            return signature;
        }

        return null;
    }
}
