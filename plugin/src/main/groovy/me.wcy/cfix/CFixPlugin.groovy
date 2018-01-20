package me.wcy.cfix

import me.wcy.cfix.utils.*
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class CFixPlugin implements Plugin<Project> {
    private static final String CFIX_DIR = "cfixDir"
    private static final String CFIX_PATCHES = "cfixPatches"

    private static final String MAPPING_TXT = "mapping.txt"
    private static final String HASH_TXT = "hash.txt"

    private static final String DEBUG = "debug"

    private Task cfixJarBeforeDexTask
    private List<File> patchList = []

    private CFixExtension extension

    @Override
    void apply(Project project) {
        project.extensions.create("cfix", CFixExtension, project)

        project.afterEvaluate {
            extension = project.extensions.findByName("cfix") as CFixExtension

            project.android.applicationVariants.each { variant ->
                if (!variant.name.contains(DEBUG) || (variant.name.contains(DEBUG) && extension.debugOn)) {
                    File cfixDir
                    File patchDir
                    Map hashMap

                    CFixLogger.init(variant)

                    // Gradle 1.5 - 2.x
                    Task dexTask2 = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
                    // Gradle 3.0+
                    Task dexTask3 = project.tasks.findByName("transformClassesWithDexBuilderFor${variant.name.capitalize()}")

                    Task dexTask
                    if (dexTask2 != null) {
                        dexTask = dexTask2
                    } else if (dexTask3 != null) {
                        dexTask = dexTask3
                    } else {
                        CFixLogger.i("Gradle Version not support")
                        return
                    }

                    Task manifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
                    Task proguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
                    Set<File> manifestFiles = manifestTask.outputs.files.files

                    File oldCFixDir = CFixFileUtils.getFileFromProperty(project, CFIX_DIR)
                    if (oldCFixDir) {
                        File mappingFile = CFixFileUtils.getVariantFile(oldCFixDir, variant, MAPPING_TXT)
                        CFixAndroidUtils.applymapping(proguardTask, mappingFile)

                        File hashFile = CFixFileUtils.getVariantFile(oldCFixDir, variant, HASH_TXT)
                        hashMap = CFixMapUtils.parseMap(hashFile)
                    }

                    String dirName = variant.dirName
                    cfixDir = new File("${project.buildDir}/outputs/cfix")
                    File outputDir = new File("${cfixDir}/${dirName}")
                    File hashFile = new File(outputDir, HASH_TXT)

                    String cfixJarBeforeDex = "cfixJarBeforeDex${variant.name.capitalize()}"
                    project.task(cfixJarBeforeDex) << {
                        Set<File> inputFiles = dexTask.inputs.files.files
                        inputFiles.each { file ->
                            CFixLogger.i("transformClassesTask input: ${file.absolutePath}")
                        }
                        Set<File> files = CFixFileUtils.getFiles(inputFiles)
                        files.each { file ->
                            if (file.name.endsWith(".jar")) {
                                CFixProcessor.processJar(file, hashFile, hashMap, patchDir, extension)
                            } else if (file.name.endsWith(".class")) {
                                CFixProcessor.processClass(file, hashFile, hashMap, patchDir, extension)
                            }
                        }
                    }

                    cfixJarBeforeDexTask = project.tasks[cfixJarBeforeDex]

                    cfixJarBeforeDexTask.doFirst {
                        CFixLogger.init(variant)

                        String applicationName = CFixAndroidUtils.getApplication(manifestFiles)
                        if (applicationName != null) {
                            extension.excludeClass.add(applicationName)
                        }

                        outputDir.deleteDir()
                        outputDir.mkdirs()
                        hashFile.createNewFile()

                        if (oldCFixDir) {
                            patchDir = new File("${cfixDir}/${dirName}/patch")
                            patchDir.mkdirs()
                            patchList.add(patchDir)
                        }
                    }

                    cfixJarBeforeDexTask.doLast {
                        if (proguardTask) {
                            File mapFile = new File("${project.buildDir}/outputs/mapping/${variant.dirName}/${MAPPING_TXT}")
                            File newMapFile = new File("${cfixDir}/${variant.dirName}/${MAPPING_TXT}")
                            FileUtils.copyFile(mapFile, newMapFile)
                        }
                    }

                    cfixJarBeforeDexTask.dependsOn dexTask.taskDependencies.getDependencies(dexTask)
                    dexTask.dependsOn cfixJarBeforeDexTask

                    String cfixPatch = "cfix${variant.name.capitalize()}Patch"
                    project.task(cfixPatch) << {
                        if (patchDir) {
                            String patchPatch = CFixAndroidUtils.dex(project, patchDir)
                            CFixAndroidUtils.signPatch(patchPatch, extension)
                        }
                    }
                    Task cfixPatchTask = project.tasks[cfixPatch]
                    cfixPatchTask.dependsOn cfixJarBeforeDexTask
                }
            }

            project.task(CFIX_PATCHES) << {
                patchList.each { patchDir ->
                    String patchPatch = CFixAndroidUtils.dex(project, patchDir)
                    CFixAndroidUtils.signPatch(patchPatch, extension)
                }
            }

            project.tasks[CFIX_PATCHES].dependsOn cfixJarBeforeDexTask
        }
    }
}
