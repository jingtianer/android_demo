package com.jingtian.asmplugin;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AppScaleVisitor extends ClassVisitor {

    String clazzName = null;
    String superClazzName = null;
    ClassWriter classWriter;

    protected AppScaleVisitor(ClassWriter classWriter) {
        super(Opcodes.ASM7, null);
        this.classWriter = classWriter;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        clazzName = name;
        superClazzName = superName;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (clazzName != null && superClazzName != null && superClazzName.equals("View") && name.equals("<init>")) {
            return new AppScaleMethodVisitor(this.api, visitor, access, name, descriptor);
        }
        return visitor;
    }
}
