package me.wcy.cfix.utils

import com.android.build.gradle.api.BaseVariant
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import me.wcy.cfix.CFixExtension
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

class CFixProcessor {
    static ClassPool classPool

    static init(Project project) {
        if (classPool == null) {
            classPool = ClassPool.default

            String sdkDir = CFixAndroidUtils.getSdkDir(project)
            if (sdkDir == null) {
                throw new InvalidUserDataException('$ANDROID_HOME is not defined')
            }

            String compileSdkVersion = project.android.compileSdkVersion
            String androidJar = "${sdkDir}/platforms/${compileSdkVersion}/android.jar"
            String apacheJar = "${sdkDir}/platforms/${compileSdkVersion}/optional/org.apache.http.legacy.jar"
            if (new File(androidJar).exists()) {
                classPool.appendClassPath(androidJar)
            }
            if (new File(apacheJar).exists()) {
                classPool.appendClassPath(apacheJar)
            }

            File hackDirFile = new File("${project.buildDir}/outputs/cfix/hack")
            hackDirFile.deleteDir()
            hackDirFile.mkdirs()
            CtClass hackClass = classPool.makeClass("me.wcy.cfix.Hack")
            hackClass.writeFile(hackDirFile.absolutePath)
            classPool.appendClassPath(hackDirFile.absolutePath)
        }
    }

    static appendClassPath(BaseVariant variant, Set<File> files) {
        files.each { file ->
            println("> cfix: input: ${file.absolutePath}")
            if (file.absolutePath.endsWith(".jar")) {
                classPool.appendClassPath(file.absolutePath)
            } else if (file.absolutePath.endsWith(".class")) {
                String[] array = CFixFileUtils.formatPath(file.absolutePath).split("/${variant.dirName}/")
                String dir = array[0] + "/" + variant.dirName
                classPool.appendClassPath(dir)
            }
        }
    }

    static processJar(File jarFile, File hashFile, Map hashMap, File patchDir, CFixExtension extension) {
        if (!jarFile.exists()) {
            return
        }

        if (shouldProcessJar(jarFile.absolutePath)) {
            println("> cfix: process jar: ${jarFile.absolutePath}")

            File optDirFile = new File(jarFile.absolutePath.substring(0, jarFile.absolutePath.length() - 4))
            optDirFile.deleteDir()
            CFixFileUtils.unZipJar(jarFile, optDirFile)
            optDirFile.eachFileRecurse { file ->
                if (file.isFile()) {
                    String classPath = file.absolutePath.substring(optDirFile.absolutePath.length() + 1)
                    if (shouldProcessClass(classPath, extension)) {
                        referHackWhenInit(optDirFile.absolutePath, classPath, hashFile, hashMap, patchDir)
                    }
                }
            }

            jarFile.delete()
            CFixFileUtils.zipJar(optDirFile, jarFile)
            optDirFile.deleteDir()
        }
    }

    static processClass(BaseVariant variant, File classFile, File hashFile, Map hashMap, File patchDir, CFixExtension extension) {
        if (!classFile.exists()) {
            return
        }

        String[] array = CFixFileUtils.formatPath(classFile.absolutePath).split("/${variant.dirName}/")
        String dir = array[0] + "/" + variant.dirName
        String classPath = array[1]
        if (shouldProcessClass(classPath, extension)) {
            referHackWhenInit(dir, classPath, hashFile, hashMap, patchDir)
        }
    }

    private static void referHackWhenInit(String dir, String classPath, File hashFile, Map hashMap,
                                          File patchDir) {
        classPath = CFixFileUtils.formatPath(classPath)
        String className = classPath.substring(0, classPath.length() - 6)
                .replace("/", ".")
        println("> cfix: process class: ${className}")
        CtClass clazz = classPool.getCtClass(className)
        if (clazz.isFrozen()) {
            clazz.defrost()
        }

        CtConstructor[] constructors = clazz.getDeclaredConstructors()
        if (constructors == null || constructors.length == 0) {
            CtConstructor constructor = new CtConstructor(new CtClass[0], clazz)
            constructor.setBody('{\nSystem.out.println(me.wcy.cfix.Hack.class);\n}')
            clazz.addConstructor(constructor)
        } else {
            CtConstructor constructor = constructors[0]
            constructor.insertBeforeBody('System.out.println(me.wcy.cfix.Hack.class);')
        }
        clazz.writeFile(dir)
        clazz.detach()

        // save hash
        File classFile = new File(dir + "/" + classPath)
        InputStream is = new FileInputStream(classFile)
        String hash = DigestUtils.sha1Hex(is)
        is.close()
        hashFile.append(CFixMapUtils.format(classPath, hash))

        if (CFixMapUtils.notSame(hashMap, classPath, hash)) {
            FileUtils.copyFile(classFile, CFixFileUtils.touchFile(patchDir, classPath))
        }
    }

    private static boolean shouldProcessJar(String jarPath) {
        jarPath = CFixFileUtils.formatPath(jarPath)
        return (jarPath.endsWith("classes.jar") || jarPath.endsWith("main.jar")) &&
                !jarPath.contains("/.android/build-cache/") &&
                !jarPath.contains("/android/m2repository/")
    }

    private static boolean shouldProcessClass(String classPath, CFixExtension extension) {
        classPath = CFixFileUtils.formatPath(classPath)
        return classPath.endsWith(".class") &&
                !classPath.startsWith("me/wcy/cfix/lib/") &&
                !classPath.contains("android/support/") &&
                !classPath.contains("/R\$") &&
                !classPath.endsWith("/R.class") &&
                !classPath.endsWith("/BuildConfig.class") &&
                CFixSetUtils.isIncluded(classPath, extension.includePackage) &&
                !CFixSetUtils.isExcluded(classPath, extension.excludeClass)
    }
}
