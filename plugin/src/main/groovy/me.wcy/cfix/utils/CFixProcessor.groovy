package me.wcy.cfix.utils

import com.android.build.gradle.api.BaseVariant
import me.wcy.cfix.CFixExtension
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.objectweb.asm.*

class CFixProcessor {

    static processJar(File jarFile, File hashFile, Map hashMap, File patchDir, CFixExtension extension) {
        if (!jarFile.exists()) {
            return
        }

        if (shouldProcessJar(jarFile.absolutePath)) {
            println("> cfix: process jar: ${jarFile.absolutePath}")

            File optDirFile = new File(jarFile.absolutePath.substring(0, jarFile.absolutePath.length() - 4))
            File metaInfoDir = new File(optDirFile, "META-INF")
            File optJar = new File(jarFile.parent, jarFile.name + ".opt")

            CFixFileUtils.unZipJar(jarFile, optDirFile)

            if (metaInfoDir.exists()) {
                metaInfoDir.deleteDir()
            }

            optDirFile.eachFileRecurse { file ->
                if (file.isFile()) {
                    String classPath = file.absolutePath.substring(optDirFile.absolutePath.length() + 1)
                    if (shouldProcessClass(classPath, extension)) {
                        referHackWhenInit(optDirFile.absolutePath, classPath, hashFile, hashMap, patchDir)
                    }
                }
            }

            CFixFileUtils.zipJar(optDirFile, optJar)
            jarFile.delete()
            optJar.renameTo(jarFile)
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
        File file = new File(dir + "/" + classPath)
        File optClass = new File(file.parent, file.name + ".opt")
        FileInputStream inputStream = new FileInputStream(file)
        FileOutputStream outputStream = new FileOutputStream(optClass)

        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
            @Override
            MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions)
                mv = new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    void visitInsn(int opcode) {
                        if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(Type.getType("Lme/wcy/cfix/Hack;"))
                        }
                        super.visitInsn(opcode)
                    }
                }
                return mv
            }
        }
        cr.accept(cv, 0)

        outputStream.write(cw.toByteArray())
        inputStream.close()
        outputStream.close()
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)

        // save hash
        FileInputStream is = new FileInputStream(file)
        String hash = DigestUtils.sha1Hex(is)
        is.close()
        hashFile.append(CFixMapUtils.format(classPath, hash))

        if (CFixMapUtils.notSame(hashMap, classPath, hash)) {
            FileUtils.copyFile(file, CFixFileUtils.touchFile(patchDir, classPath))
        }
    }

    private static boolean shouldProcessJar(String jarPath) {
        jarPath = CFixFileUtils.formatPath(jarPath)
        return (jarPath.endsWith("/classes.jar") || jarPath.endsWith("/main.jar")) &&
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
