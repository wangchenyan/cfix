package me.wcy.cfix.utils

import org.apache.commons.io.FileUtils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

class CFixFileUtils {

    static File touchFile(File dir, String path) {
        def file = new File("${dir}/${path}")
        file.getParentFile().mkdirs()
        return file
    }

    static copyBytesToFile(byte[] bytes, File file) {
        if (!file.exists()) {
            file.createNewFile()
        }
        FileUtils.writeByteArrayToFile(file, bytes)
    }

    static File getFileFromProperty(Project project, String property) {
        def file
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

    static void getSingleFiles(Set<File> files, Set<File> singleFiles) {
        for (def file : files) {
            getSingleFilesLoop(file, singleFiles)
        }
    }

    private static void getSingleFilesLoop(File file, Set<File> singleFiles) {
        if (file == null || !file.exists()) {
            return
        }
        if (file.isFile()) {
            singleFiles.add(file)
            return
        }
        if (file.isDirectory()) {
            def listFiles = file.listFiles()
            for (def f : listFiles) {
                getSingleFilesLoop(f, singleFiles)
            }
        }
    }
}
