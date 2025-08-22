package com.jingtian.asmplugin;


import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class CanvasVisitor extends ClassVisitor {

    String clazzName = null;
    String superClazzName = null;

    protected CanvasVisitor(ClassWriter classWriter) {
        super(Opcodes.ASM7, classWriter);
    }

    private void printLog(String str) {
        Logger.printLog("[CanvasVisitor] " + str);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        clazzName = name;
        superClazzName = superName;
    }

    private void printError(Exception e, String msg) {
        ByteArrayOutputStream bim = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bim);
        e.printStackTrace(pw);
        printLog("[error]: msg = " + msg + ", type = " + e.getClass().getSimpleName() + ", message: " + e.getMessage() + ", stacktrace = " + bim.toString());
    }

    @Override
    public void visitEnd() {
        if ("android/graphics/Canvas".equals(clazzName)) {

            Method method = Method.getMethod("boolean clipRect(float left, float top, float right, float bottom, int nativeOp)");
            GeneratorAdapter generatorAdapter = new GeneratorAdapter(
                    ACC_PUBLIC, method, null, null, this
            );
            try {
                printLog("api = " + this.api + ", visitor = " + this + ", method = " + method);
                addClipRect(generatorAdapter);
            } catch (Exception e) {
                printError(e, "at CanvasVisitor");
                throw e;
            }
        }
        super.visitEnd();
    }

    private void addClipRect(MethodVisitor methodVisitor) {
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitLineNumber(13, label0);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitFieldInsn(GETFIELD, "android/graphics/Canvas", "mNativeCanvasWrapper", "J");
        methodVisitor.visitVarInsn(FLOAD, 1);
        methodVisitor.visitVarInsn(FLOAD, 2);
        methodVisitor.visitVarInsn(FLOAD, 3);
        methodVisitor.visitVarInsn(FLOAD, 4);
        methodVisitor.visitVarInsn(ILOAD, 5);
        methodVisitor.visitMethodInsn(INVOKESTATIC, "android/graphics/Canvas", "nClipRect", "(JFFFFI)Z", false);
        methodVisitor.visitInsn(POP);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(14, label1);
        methodVisitor.visitInsn(RETURN);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLocalVariable("this", "Landroid/graphics/Canvas;", null, label0, label2, 0);
        methodVisitor.visitLocalVariable("left", "F", null, label0, label2, 1);
        methodVisitor.visitLocalVariable("top", "F", null, label0, label2, 2);
        methodVisitor.visitLocalVariable("right", "F", null, label0, label2, 3);
        methodVisitor.visitLocalVariable("bottom", "F", null, label0, label2, 4);
        methodVisitor.visitLocalVariable("regionOp", "I", null, label0, label2, 5);
        methodVisitor.visitMaxs(7, 6);
    }
}
