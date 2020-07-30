package io.smallrye.config;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
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

import io.smallrye.common.classloader.ClassDefiner;

final class ConfigMappingClass {
    private static final ClassValue<Class<?>> cv = new ClassValue<Class<?>>() {
        @Override
        protected Class<?> computeValue(final Class<?> type) {
            return createConfigurationClass(type);
        }
    };

    private static final String I_OBJECT = getInternalName(Object.class);

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
                    getMethodDescriptor(getType(declaredField.getType())), null, null);
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

            ctor.visitVarInsn(ALOAD, 1);
            ctor.visitVarInsn(ALOAD, 0);
            ctor.visitMethodInsn(INVOKEINTERFACE, interfaceInternalName, name, getMethodDescriptor(getType(type)),
                    true);
            ctor.visitFieldInsn(PUTFIELD, classInternalName, name, getDescriptor(type));
        }

        ctor.visitVarInsn(ALOAD, 1);
        ctor.visitInsn(ARETURN);
        ctor.visitMaxs(2, 2);
        writer.visitEnd();

        return ClassDefiner.defineClass(MethodHandles.lookup(), ConfigMappingClass.class, interfaceName,
                writer.toByteArray());
    }
}
