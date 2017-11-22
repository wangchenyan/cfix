package me.wcy.cfix

import me.wcy.cfix.utils.CFixAndroidUtils
import me.wcy.cfix.utils.CFixFileUtils
import me.wcy.cfix.utils.CFixMapUtils
import me.wcy.cfix.utils.CFixProcessor
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

                    Task transformClassesAndResourcesWithProguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
                    Task transformClassesWithDexTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")

                    Task processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
                    File manifestFile = processManifestTask.outputs.files.files[1]

                    File oldCFixDir = CFixFileUtils.getFileFromProperty(project, CFIX_DIR)
                    if (oldCFixDir) {
                        File mappingFile = CFixFileUtils.getVariantFile(oldCFixDir, variant, MAPPING_TXT)
                        CFixAndroidUtils.applymapping(transformClassesAndResourcesWithProguardTask, mappingFile)

                        File hashFile = CFixFileUtils.getVariantFile(oldCFixDir, variant, HASH_TXT)
                        hashMap = CFixMapUtils.parseMap(hashFile)
                    }

                    String dirName = variant.dirName
                    cfixDir = new File("${project.buildDir}/outputs/cfix")
                    File outputDir = new File("${cfixDir}/${dirName}")
                    File hashFile = new File(outputDir, HASH_TXT)

                    String cfixJarBeforeDex = "cfixJarBeforeDex${variant.name.capitalize()}"
                    project.task(cfixJarBeforeDex) << {
                        Set<File> inputFiles = transformClassesWithDexTask.inputs.files.files
                        inputFiles.each { file ->
                            println("> cfix: input: ${file.absolutePath}")
                        }
                        Set<File> files = CFixFileUtils.getFiles(inputFiles)
                        files.each { file ->
                            if (file.absolutePath.endsWith(".jar")) {
                                CFixProcessor.processJar(file, hashFile, hashMap, patchDir, extension)
                            } else if (file.absolutePath.endsWith(".class")) {
                                CFixProcessor.processClass(variant, file, hashFile, hashMap, patchDir, extension)
                            }
                        }
                    }

                    cfixJarBeforeDexTask = project.tasks[cfixJarBeforeDex]

                    cfixJarBeforeDexTask.doFirst {
                        println("> cfix: variant: ${variant.name}")

                        CFixProcessor.init(project)

                        String applicationName = CFixAndroidUtils.getApplication(manifestFile)
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
                        if (transformClassesAndResourcesWithProguardTask) {
                            File mapFile = new File("${project.buildDir}/outputs/mapping/${variant.dirName}/mapping.txt")
                            File newMapFile = new File("${cfixDir}/${variant.dirName}/mapping.txt")
                            FileUtils.copyFile(mapFile, newMapFile)
                        }
                    }

                    cfixJarBeforeDexTask.dependsOn transformClassesWithDexTask.taskDependencies.getDependencies(transformClassesWithDexTask)
                    transformClassesWithDexTask.dependsOn cfixJarBeforeDexTask

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
