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

import org.apache.commons.compress.utils.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

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
        Logger.printLog("[Transform] " + str);
    }

    private void printError(Exception e, String extra) {
        ByteArrayOutputStream bim = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(bim);
        e.printStackTrace(pw);
        printLog("[error]: " + extra + ", type = " + e.getClass().getSimpleName() + ", message: " + e.getMessage() + ", stacktrace = " + bim.toString());
    }

    private void printError(Exception e) {
        printError(e, "");
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {
        printLog("start transform");
        Collection<TransformInput> transformInputs = transformInvocation.getInputs();
        TransformOutputProvider transformOutputProvider = transformInvocation.getOutputProvider();
        for (TransformInput input : transformInputs) {
            Collection<DirectoryInput> directoryInputs = input.getDirectoryInputs();
            for (DirectoryInput directoryInput : directoryInputs) {
                File dir = directoryInput.getFile();
                printLog("read dir " + dir.getAbsolutePath());
                if (!dir.isDirectory()) {
                    continue;
                }
                FluentIterable<File> allFiles = FileUtils.getAllFiles(dir);
                for (File file : allFiles) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".class")) {
                        printLog("file = " + fileName);
                        String clazzPath = file.getAbsolutePath();
                        byte[] transformedClass = null;
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ClassReader classReader = new ClassReader(fis.readAllBytes());
                            ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                            ClassVisitor classVisitor = new AppScaleVisitor(classWriter);
                            try {
//                                classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                                try {
                                    CheckClassAdapter checkAdapter = new CheckClassAdapter(classVisitor);
                                    classReader.accept(checkAdapter, 0);
                                    transformedClass = classWriter.toByteArray();
                                } catch (Exception e) {
                                    printError(e, "invalid bytecode");
                                }
                            } catch (Exception e) {
                                printError(e, "at AppScaleVisitor.accept");
                            }
                        } catch (Exception e) {
                            printError(e);
                        }
                        if (transformedClass != null) {
                            try (FileOutputStream fos = new FileOutputStream(clazzPath)) {
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
                File srcJar = jarInput.getFile();
                File destJar = transformOutputProvider.getContentLocation(
                        jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR
                );
//                FileUtils.copyFile(srcJar, destJar);
                try (JarFile srcJarFile = new JarFile(srcJar); JarOutputStream destJarFileOs = new JarOutputStream(new FileOutputStream(destJar))) {
                    Enumeration<JarEntry> enumeration = srcJarFile.entries();
                    //遍历srcJar中的每一条条目
                    while (enumeration.hasMoreElements()) {
                        JarEntry entry = enumeration.nextElement();
                        try (InputStream entryIs = srcJarFile.getInputStream(entry)) {
                            byte[] bytes = null;
                            if (entry.getName().endsWith(".class")) {//如果是class文件
                                try {
                                    //通过asm修改源class文件
                                    ClassReader classReader = new ClassReader(entryIs);
                                    ClassWriter classWriter = new ClassWriter(classReader, 0);
                                    AppScaleVisitor appScaleVisitor = new AppScaleVisitor(classWriter);
                                    try {
                                        try {
                                            CheckClassAdapter checkAdapter = new CheckClassAdapter(appScaleVisitor);
                                            classReader.accept(checkAdapter, 0);
                                            bytes = classWriter.toByteArray();
                                        } catch (Exception e) {
                                            printError(e, "invalid bytecode, " + "jar = " + srcJarFile.getName() + ", entry = " + entry.getName());
                                        }
                                    } catch (Exception e) {
                                        printError(e, "at AppScaleVisitor.accept, " + "jar = " + srcJarFile.getName() + ", entry = " + entry.getName());
                                    }
                                } catch (Exception e) {
                                    printError(e);
                                }
                            }
                            if (bytes != null) {
                                destJarFileOs.putNextEntry(new JarEntry(entry.getName()));
                                destJarFileOs.write(bytes);
                            }
                        } catch (Exception e) {
                            printError(e);
                        } finally {
                            destJarFileOs.closeEntry();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void apply(Project target) {
        printLog("registerTransform");
        target.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                printLog("afterEvaluate");
                AppExtension appExtension = project.getExtensions().getByType(AppExtension.class);
                appExtension.registerTransform(AppScaleAsmPlugin.this);
            }
        });
    }
}
