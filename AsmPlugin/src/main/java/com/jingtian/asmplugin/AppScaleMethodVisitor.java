package com.jingtian.asmplugin;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

public class AppScaleMethodVisitor extends AdviceAdapter{
    protected AppScaleMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodEnter() {
        MethodVisitor methodVisitor = mv;
        Label label1 = new Label();
        methodVisitor.visitLabel(label1);
        methodVisitor.visitLineNumber(10, label1);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(FCONST_2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "com/jingtian/demoapp/main/widget/TestView", "setScaleX", "(F)V", false);
        Label label2 = new Label();
        methodVisitor.visitLabel(label2);
        methodVisitor.visitLineNumber(11, label2);
        methodVisitor.visitVarInsn(ALOAD, 0);
        methodVisitor.visitInsn(FCONST_2);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "com/jingtian/demoapp/main/widget/TestView", "setScaleY", "(F)V", false);
    }
}
