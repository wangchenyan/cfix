package me.wcy.cfix.utils

import me.wcy.cfix.CFixExtension
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.objectweb.asm.*

class CFixProcessor {

    static processJar(File jarFile, File hashFile, Map hashMap, File patchDir, CFixExtension extension) {
        if (shouldProcessJar(jarFile)) {
            CFixLogger.i("process jar: ${jarFile.absolutePath}")

            File optDirFile = new File(jarFile.absolutePath.substring(0, jarFile.absolutePath.length() - 4))
            CFixFileUtils.unZipJar(jarFile, optDirFile)

            File metaInfoDir = new File(optDirFile, "META-INF")
            if (metaInfoDir.exists()) {
                metaInfoDir.deleteDir()
            }

            int counter = 0
            optDirFile.eachFileRecurse { file ->
                if (file.isFile()) {
                    boolean result = processClass(file, hashFile, hashMap, patchDir, extension)
                    if (result) {
                        counter++
                    }
                }
            }

            if (counter == 0) {
                optDirFile.deleteDir()
                return
            }

            File optJar = new File(jarFile.parent, jarFile.name + ".opt")
            CFixFileUtils.zipJar(optDirFile, optJar)
            jarFile.delete()
            optJar.renameTo(jarFile)
            optDirFile.deleteDir()
        }
    }

    static boolean processClass(File classFile, File hashFile, Map hashMap, File patchDir, CFixExtension extension) {
        if (shouldProcessClass(classFile, extension)) {
            referHackWhenInit(classFile, hashFile, hashMap, patchDir)
            return true
        }
        return false
    }

    private static void referHackWhenInit(File classFile, File hashFile, Map hashMap,
                                          File patchDir) {
        File optClass = new File(classFile.parent, classFile.name + ".opt")
        FileInputStream inputStream = new FileInputStream(classFile)
        FileOutputStream outputStream = new FileOutputStream(optClass)

        ClassReader cr = new ClassReader(inputStream)
        String className = cr.className
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
        if (classFile.exists()) {
            classFile.delete()
        }
        optClass.renameTo(classFile)

        // save hash
        FileInputStream is = new FileInputStream(classFile)
        String hash = DigestUtils.sha1Hex(is)
        is.close()
        hashFile.append(CFixMapUtils.format(className, hash))

        if (CFixMapUtils.notSame(hashMap, className, hash)) {
            FileUtils.copyFile(classFile, CFixFileUtils.touchFile(patchDir, className + ".class"))
        }
    }

    private static boolean shouldProcessJar(File jarFile) {
        if (!jarFile.exists() || !jarFile.name.endsWith(".jar")) {
            return false
        }

        String jarPath = CFixFileUtils.formatPath(jarFile.absolutePath)
        return jarPath.contains("/build/intermediates/")
    }

    private static boolean shouldProcessClass(File classFile, CFixExtension extension) {
        if (!classFile.exists() || !classFile.name.endsWith(".class")) {
            return false
        }

        FileInputStream inputStream = new FileInputStream(classFile)
        ClassReader cr = new ClassReader(inputStream)
        String className = cr.className
        inputStream.close()

        return !className.startsWith("me/wcy/cfix/lib/") &&
                !className.contains("android/support/") &&
                !className.contains("/R\$") &&
                !className.endsWith("/R") &&
                !className.endsWith("/BuildConfig") &&
                CFixSetUtils.isIncluded(className, extension.includePackage) &&
                !CFixSetUtils.isExcluded(className, extension.excludeClass)
    }
}
