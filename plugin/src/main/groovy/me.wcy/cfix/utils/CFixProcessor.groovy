package me.wcy.cfix.utils

import com.android.build.gradle.api.BaseVariant
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Created by jixin.jia on 15/11/10.
 */
class CFixProcessor {

    public static processJar(File jarFile, File hashFile, Map hashMap, File patchDir,
                             HashSet<String> includePackage, HashSet<String> excludeClass) {
        if (shouldProcessJar(jarFile)) {
            println("> cfix: process jar: ${jarFile.absolutePath}")

            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")

            def file = new JarFile(jarFile);
            Enumeration enumeration = file.entries();
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);

                InputStream inputStream = file.getInputStream(jarEntry);
                jarOutputStream.putNextEntry(zipEntry);

                if (shouldProcessClass(entryName, includePackage, excludeClass)) {
                    def bytes = referHackWhenInit(inputStream);
                    jarOutputStream.write(bytes);

                    def hash = DigestUtils.shaHex(bytes)
                    hashFile.append(CFixMapUtils.format(entryName, hash))

                    if (CFixMapUtils.notSame(hashMap, entryName, hash)) {
                        CFixFileUtils.copyBytesToFile(bytes, CFixFileUtils.touchFile(patchDir, entryName))
                    }
                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            jarOutputStream.close();
            file.close();

            if (jarFile.exists()) {
                jarFile.delete()
            }
            optJar.renameTo(jarFile)
        }
    }

    public static processClass(BaseVariant variant, File classFile, File hashFile, Map hashMap,
                               File patchDir, HashSet<String> includePackage, HashSet<String> excludeClass) {
        if (classFile == null || !classFile.exists()) {
            return
        }

        String name = classFile.absolutePath.replace("\\", "/").split("${variant.dirName}/")[1]
        if (shouldProcessClass(name, includePackage, excludeClass)) {
            println("> cfix: process class: ${classFile.absolutePath}")

            def optClass = new File(classFile.getParent(), classFile.name + ".opt")

            FileInputStream inputStream = new FileInputStream(classFile);
            FileOutputStream outputStream = new FileOutputStream(optClass)

            def bytes = referHackWhenInit(inputStream);
            outputStream.write(bytes)
            inputStream.close()
            outputStream.close()
            if (classFile.exists()) {
                classFile.delete()
            }
            optClass.renameTo(classFile)

            def hash = DigestUtils.shaHex(bytes)
            hashFile.append(CFixMapUtils.format(name, hash))

            if (CFixMapUtils.notSame(hashMap, name, hash)) {
                CFixFileUtils.copyBytesToFile(bytes, CFixFileUtils.touchFile(patchDir, name))
            }
        }
    }

    //refer hack class when object init
    private static byte[] referHackWhenInit(InputStream inputStream) {
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM4, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv = new MethodVisitor(Opcodes.ASM4, mv) {
                    @Override
                    void visitInsn(int opcode) {
                        if ("<init>".equals(name) && opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(Type.getType("Lme/wcy/cfix/Hack;"));
                        }
                        super.visitInsn(opcode);
                    }
                }
                return mv;
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    public static boolean shouldProcessJar(File jarFile) {
        if (jarFile == null || !jarFile.exists()) {
            return false
        }

        String formatPath = jarFile.absolutePath.replace("\\", "/")
        return (formatPath.endsWith("classes.jar") || formatPath.endsWith("main.jar")) &&
                !formatPath.contains("/.android/build-cache/") &&
                !formatPath.contains("/android/m2repository/");
    }

    private static boolean shouldProcessClass(String name,
                                              HashSet<String> includePackage, HashSet<String> excludeClass) {
        return name.endsWith(".class") &&
                !name.startsWith("me/wcy/cfix/lib/") &&
                !name.contains("android/support/") &&
                !name.contains("/R\$") &&
                !name.endsWith("/R.class") &&
                !name.endsWith("/BuildConfig.class") &&
                CFixSetUtils.isIncluded(name, includePackage) &&
                !CFixSetUtils.isExcluded(name, excludeClass)
    }
}
