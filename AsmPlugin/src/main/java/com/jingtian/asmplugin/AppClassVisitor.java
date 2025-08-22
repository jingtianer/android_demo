package com.jingtian.asmplugin;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

public class AppClassVisitor extends ClassVisitor {

    String clazzName = null;
    String superClazzName = null;

    protected AppClassVisitor(ClassVisitor classWriter) {
        super(Opcodes.ASM7, classWriter);
    }

    private void printLog(String str) {
        Logger.printLog("[ClassVisitor] " + str);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        clazzName = name;
        superClazzName = superName;
//        printLog("visit: clazzName = " + clazzName + ", superClazzName = " + superClazzName);
    }

    private void printError(Exception e) {
        ByteArrayOutputStream bim = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bim);
        e.printStackTrace(pw);
        printLog("[error]: " + "type = " + e.getClass().getSimpleName() + ", message: " + e.getMessage() + ", stacktrace = " + bim.toString());
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
//        printLog("visitMethod: " + "methodName = " + name + " clazzName = " + clazzName + ", superClazzName = " + superClazzName);
        if (clazzName != null && superClazzName != null && superClazzName.equalsIgnoreCase("android/app/Activity") && name.equals("onCreate")) {
            printLog("visitMethod: process Method" + "methodName = " + name + " clazzName = " + clazzName + ", superClazzName = " + superClazzName);
            try {
                printLog("api = " + this.api + ", visitor = " + visitor + ", access = " + access + ", name = " + name + ", descriptor = " + descriptor);
                return new ActivityToastMethodVisitor(this.api, visitor, access, name, descriptor);
            } catch (Exception e) {
                printError(e);
            }
        }
        return visitor;
    }
}
