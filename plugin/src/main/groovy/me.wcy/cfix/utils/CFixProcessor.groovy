package me.wcy.cfix.utils

import com.android.build.gradle.api.BaseVariant
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import me.wcy.cfix.CFixExtension
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class CFixProcessor {
    static ClassPool classPool

    static init(Project project) {
        if (classPool == null) {
            classPool = ClassPool.default

            File hackDirFile = new File("${project.buildDir}/outputs/cfix/hack")
            hackDirFile.deleteDir()
            hackDirFile.mkdirs()
            CtClass hackClass = classPool.makeClass("me.wcy.cfix.Hack")
            hackClass.writeFile(hackDirFile.absolutePath)
            classPool.appendClassPath(hackDirFile.absolutePath)
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
            classPool.appendClassPath(optDirFile.absolutePath)
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
            println("> cfix: process class: ${classFile.absolutePath}")

            classPool.appendClassPath(dir)
            referHackWhenInit(dir, classPath, hashFile, hashMap, patchDir)
        }
    }

    private static void referHackWhenInit(String dir, String classPath, File hashFile, Map hashMap,
                                          File patchDir) {
        classPath = CFixFileUtils.formatPath(classPath)
        String className = classPath.substring(0, classPath.length() - 6)
                .replace("/", ".")
        CtClass clazz = classPool.getCtClass(className)
        if (clazz.isFrozen()) {
            clazz.defrost()
        }

        CtConstructor[] constructors = clazz.getConstructors()
        if (constructors.length > 0) {
            CtConstructor constructor = constructors[0]
            constructor.insertBeforeBody("Class cls = me.wcy.cfix.Hack.class;")
            clazz.writeFile(dir)
            clazz.detach()

            // save hash
            File classFile = new File(dir + "/" + classPath)
            InputStream is = new FileInputStream(classFile)
            String hash = DigestUtils.shaHex(is)
            is.close()
            hashFile.append(CFixMapUtils.format(classPath, hash))

            if (CFixMapUtils.notSame(hashMap, classPath, hash)) {
                FileUtils.copyFile(classFile, CFixFileUtils.touchFile(patchDir, classPath))
            }
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
