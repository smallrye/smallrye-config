package io.smallrye.config.mapper;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 *
 */
final class Debugging {
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

        public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature,
                final String[] exceptions) {
            return new MethodVisitorImpl(api, super.visitMethod(access, name, descriptor, signature, exceptions));
        }
    }
}
