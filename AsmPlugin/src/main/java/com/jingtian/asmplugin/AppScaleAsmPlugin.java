package com.jingtian.asmplugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;
import com.google.common.collect.FluentIterable;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import groovy.lang.Closure;

public class AppScaleAsmPlugin extends Transform implements Plugin<Project> {
    @Override
    public String getName() {
        return "AppScaleAsmPlugin";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    private void printLog(String str) {
        System.out.printf("[AppScaleAsmPlugin]: %s%n", str);
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {
        printLog("transform");
        Collection<TransformInput> transformInputs = transformInvocation.getInputs();
        TransformOutputProvider transformOutputProvider =  transformInvocation.getOutputProvider();
        for (TransformInput input : transformInputs) {
            Collection<DirectoryInput> directoryInputs = input.getDirectoryInputs();
            for (DirectoryInput directoryInput : directoryInputs) {
                File dir = directoryInput.getFile();
                printLog(dir.getName());
                if (!dir.isDirectory()) {
                    continue;
                }
                FluentIterable<File> allFiles = FileUtils.getAllFiles(dir);
                for (File file : allFiles) {
                    String fileName = file.getName();
                    printLog(fileName);
                    if (true) {
                        String clazzPath = file.getAbsolutePath();
                        byte[] transformedClass = null;
                        try (FileInputStream fis = new FileInputStream(file)){
                            ClassReader classReader = new ClassReader(fis.readAllBytes());
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                            ClassVisitor classVisitor = new AppScaleVisitor(classWriter);
                            classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                            transformedClass = classWriter.toByteArray();
                        }
                        if (transformedClass != null) {
                            try (FileOutputStream fos = new FileOutputStream(clazzPath)){
                                fos.write(transformedClass);
                            }
                        }
                    }
                }
                File dest = transformOutputProvider.getContentLocation(
                        directoryInput.getName(),
                        directoryInput.getContentTypes(),
                        directoryInput.getScopes(),
                        Format.DIRECTORY
                );
                FileUtils.copyDirectoryToDirectory(directoryInput.getFile(), dest);
            }
            Collection<JarInput> jarInputs = input.getJarInputs();
            for (JarInput jarInput : jarInputs) {
                File dest = transformOutputProvider.getContentLocation(
                        jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR
                );
                FileUtils.copyFile(jarInput.getFile(), dest);
            }
        }
    }

    @Override
    public void apply(Project target) {
        AppExtension appExtension = target.getExtensions().getByType(AppExtension.class);
        printLog("registerTransform");
        appExtension.registerTransform(this);
    }
}
