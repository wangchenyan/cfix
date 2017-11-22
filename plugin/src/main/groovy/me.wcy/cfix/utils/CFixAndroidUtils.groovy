package me.wcy.cfix.utils

import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ProGuardTransform
import groovy.xml.Namespace
import me.wcy.cfix.CFixExtension
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project

class CFixAndroidUtils {
    private static final String PATCH_NAME = "patch.jar"

    static String getApplication(File manifestFile) {
        Node manifest = new XmlParser().parse(manifestFile)
        Namespace androidTag = new Namespace("http://schemas.android.com/apk/res/android", 'android')
        String applicationName = manifest.application[0].attribute(androidTag.name)

        if (applicationName != null) {
            return applicationName.replace(".", "/") + ".class"
        }
        return null
    }

    static String dex(Project project, File classDir) {
        String patchPath = classDir.getParent() + "/" + PATCH_NAME
        if (classDir.listFiles().size()) {
            String sdkDir
            Properties properties = new Properties()
            File localProps = project.rootProject.file("local.properties")
            if (localProps.exists()) {
                properties.load(localProps.newDataInputStream())
                sdkDir = properties.getProperty("sdk.dir")
            } else {
                sdkDir = System.getenv("ANDROID_HOME")
            }
            if (sdkDir) {
                String cmdExt = Os.isFamily(Os.FAMILY_WINDOWS) ? '.bat' : ''
                ByteArrayOutputStream stdout = new ByteArrayOutputStream()
                project.exec {
                    commandLine "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExt}",
                            '--dex',
                            "--output=${patchPath}",
                            "${classDir.absolutePath}"
                    standardOutput = stdout
                }
                String error = stdout.toString().trim()
                if (error) {
                    println "dex error:" + error
                }
            } else {
                throw new InvalidUserDataException('$ANDROID_HOME is not defined')
            }
        }
        return patchPath
    }

    static applymapping(TransformTask proguardTask, File mappingFile) {
        if (proguardTask) {
            ProGuardTransform transform = (ProGuardTransform) proguardTask.getTransform()
            if (mappingFile.exists()) {
                transform.applyTestedMapping(mappingFile)
            } else {
                println "${mappingFile} does not exist"
            }
        }
    }

    static signPatch(String patchPath, CFixExtension extension) {
        File patchFile = new File(patchPath)
        if (!patchFile.exists() || !extension.sign) {
            return
        }

        if (extension.storeFile == null || !extension.storeFile.exists()) {
            throw new IllegalArgumentException("> cfix: store file not exists")
        }

        println("> cfix: sign patch")

        List<String> command = [JavaEnvUtils.getJdkExecutable('jarsigner'),
                                '-verbose',
                                '-sigalg', 'MD5withRSA',
                                '-digestalg', 'SHA1',
                                '-keystore', extension.storeFile.absolutePath,
                                '-keypass', extension.keyPassword,
                                '-storepass', extension.storePassword,
                                patchFile.absolutePath,
                                extension.keyAlias]
        Process proc = command.execute()

        Thread outThread = new Thread(new Runnable() {
            @Override
            void run() {
                int b
                while ((b = proc.inputStream.read()) != -1) {
                    System.out.write(b)
                }
            }
        })
        Thread errThread = new Thread(new Runnable() {
            @Override
            void run() {
                int b
                while ((b = proc.errorStream.read()) != -1) {
                    System.out.write(b)
                }
            }
        })

        outThread.start()
        errThread.start()

        int result = proc.waitFor()
        outThread.join()
        errThread.join()

        if (result != 0) {
            throw new GradleException('> cfix: sign failed')
        }
    }
}
