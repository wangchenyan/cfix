package me.wcy.cfix.utils

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class CFixFileUtils {

    static File touchFile(File dir, String path) {
        File file = new File("${dir}/${path}")
        file.getParentFile().mkdirs()
        return file
    }

    static File getFileFromProperty(Project project, String property) {
        File file = null
        if (project.hasProperty(property)) {
            file = new File(project.getProperties()[property])
            if (!file.exists()) {
                throw new InvalidUserDataException("${project.getProperties()[property]} does not exist")
            }
            if (!file.isDirectory()) {
                throw new InvalidUserDataException("${project.getProperties()[property]} is not directory")
            }
        }
        return file
    }

    static File getVariantFile(File dir, def variant, String fileName) {
        return new File("${dir}/${variant.dirName}/${fileName}")
    }

    static Set<File> getFiles(Set<File> inputFiles) {
        Set<File> files = []
        for (File file : inputFiles) {
            if (file.directory) {
                file.eachFileRecurse { f ->
                    if (f.file) {
                        files.add(f)
                    }
                }
            } else if (file.file) {
                files.add(file)
            }
        }
        return files
    }

    static void unZipJar(File jar, File dest) {
        JarFile jarFile = new JarFile(jar)
        Enumeration<JarEntry> jarEntries = jarFile.entries()
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement()
            if (jarEntry.directory) {
                continue
            }
            String entryName = jarEntry.getName()
            String outFileName = dest.absolutePath + "/" + entryName
            File outFile = new File(outFileName)
            outFile.parentFile.mkdirs()
            InputStream inputStream = jarFile.getInputStream(jarEntry)
            FileOutputStream fileOutputStream = new FileOutputStream(outFile)
            fileOutputStream << inputStream
            fileOutputStream.close()
            inputStream.close()
        }
        jarFile.close()
    }

    static void zipJar(File jarDir, File dest) {
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(dest))
        jarDir.eachFileRecurse { f ->
            if (!f.directory) {
                String entryName = f.absolutePath.substring(jarDir.absolutePath.length() + 1)
                outputStream.putNextEntry(new ZipEntry(entryName))
                InputStream inputStream = new FileInputStream(f)
                outputStream << inputStream
                inputStream.close()
            }
        }
        outputStream.close()
    }

    static String formatPath(String path) {
        return path.replace("\\", "/")
    }
}
