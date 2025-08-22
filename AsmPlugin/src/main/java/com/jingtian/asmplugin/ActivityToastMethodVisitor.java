package com.jingtian.asmplugin;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class ActivityToastMethodVisitor extends AdviceAdapter {
    protected ActivityToastMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodEnter() {
        try {
            MethodVisitor methodVisitor = this;
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLineNumber(13, label2);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getSimpleName", "()Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitLdcInsn("\u63d2\u88c5\u6210\u529f\uff01");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            methodVisitor.visitInsn(ICONST_1);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "android/widget/Toast", "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "android/widget/Toast", "show", "()V", false);
        } catch (Exception e) {
            printError(e);
            throw e;
        }
    }

    private void printLog(String str) {
        Logger.printLog("[MethodVisitor] " + str);
    }

    private void printError(Exception e) {
        ByteArrayOutputStream bim = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bim);
        e.printStackTrace(pw);
        printLog("[error]: " + "type = " + e.getClass().getSimpleName() + ", message: " + e.getMessage() + ", stacktrace = " + bim.toString());
    }
}
