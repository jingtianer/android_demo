package com.jingtian.asmplugin;


import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;

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

    private static final String CANVAS_CLASS_NAME = "android/graphics/Canvas";
    private static final String CANVAS_CLASS_LABEL = "Landroid/graphics/Canvas;";
    private static final String VIEW_CLASS_NAME = "android/view/View";

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
        if (VIEW_CLASS_NAME.equals(superClazzName)) {
            Method method = Method.getMethod("boolean clipRect(android.graphics.Canvas,float,float,float,float,int)");
            GeneratorAdapter generatorAdapter = new GeneratorAdapter(
                    ACC_PUBLIC, method, null, null, this
            );
            try {
                printLog("api = " + this.api + ", clazzName = " + clazzName + ", superClazzName = " + superClazzName + ", visitor = " + this + ", method = " + method);
                addClipRect(generatorAdapter, clazzName);
            } catch (Exception e) {
                printError(e, "at CanvasVisitor");
                throw e;
            }
        }
        super.visitEnd();
    }

    private void addClipRect(MethodVisitor methodVisitor, String clazzName) {
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(ALOAD, 1);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, CANVAS_CLASS_NAME, "getNativeCanvasWrapper", "()J", false);
        methodVisitor.visitVarInsn(FLOAD, 2);
        methodVisitor.visitVarInsn(FLOAD, 3);
        methodVisitor.visitVarInsn(FLOAD, 4);
        methodVisitor.visitVarInsn(FLOAD, 5);
        methodVisitor.visitVarInsn(ILOAD, 6);
        methodVisitor.visitMethodInsn(INVOKESTATIC, CANVAS_CLASS_NAME, "nClipRect", "(JFFFFI)Z", false);
        methodVisitor.visitInsn(IRETURN);
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLocalVariable("this", "L" + clazzName + ";", null, label0, label1, 0);
        methodVisitor.visitLocalVariable("canvas", CANVAS_CLASS_LABEL, null, label0, label1, 1);
        methodVisitor.visitLocalVariable("left", "F", null, label0, label1, 2);
        methodVisitor.visitLocalVariable("top", "F", null, label0, label1, 3);
        methodVisitor.visitLocalVariable("right", "F", null, label0, label1, 4);
        methodVisitor.visitLocalVariable("bottom", "F", null, label0, label1, 5);
        methodVisitor.visitLocalVariable("regionOp", "I", null, label0, label1, 6);
        methodVisitor.visitMaxs(7, 7);
    }
}
