package me.wcy.cfix

import me.wcy.cfix.utils.CFixAndroidUtils
import me.wcy.cfix.utils.CFixFileUtils
import me.wcy.cfix.utils.CFixMapUtils
import me.wcy.cfix.utils.CFixProcessor
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class CFixPlugin implements Plugin<Project> {
    private static final String CFIX_DIR = "CFixDir"
    private static final String CFIX_PATCHES = "cfixPatches"

    private static final String MAPPING_TXT = "mapping.txt"
    private static final String HASH_TXT = "hash.txt"

    private static final String DEBUG = "debug"

    private HashSet<String> includePackage
    private HashSet<String> excludeClass
    private def debugOn

    private def cfixJarBeforeDexTask
    private def patchList = []

    @Override
    void apply(Project project) {
        project.extensions.create("cfix", CFixExtension, project)

        project.afterEvaluate {
            def extension = project.extensions.findByName("cfix") as CFixExtension
            includePackage = extension.includePackage
            excludeClass = extension.excludeClass
            debugOn = extension.debugOn

            project.android.applicationVariants.each { variant ->
                println("> variant: ${variant.name}")
                if (!variant.name.contains(DEBUG) || (variant.name.contains(DEBUG) && debugOn)) {
                    Map hashMap
                    File cfixDir
                    File patchDir

                    def transformClassesAndResourcesWithProguardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}")
                    def transformClassesWithDexTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")

                    def processManifestTask = project.tasks.findByName("process${variant.name.capitalize()}Manifest")
                    def manifestFile = processManifestTask.outputs.files.files[1]

                    def oldCFixDir = CFixFileUtils.getFileFromProperty(project, CFIX_DIR)
                    if (oldCFixDir) {
                        def mappingFile = CFixFileUtils.getVariantFile(oldCFixDir, variant, MAPPING_TXT)
                        CFixAndroidUtils.applymapping(transformClassesAndResourcesWithProguardTask, mappingFile)
                    }
                    if (oldCFixDir) {
                        def hashFile = CFixFileUtils.getVariantFile(oldCFixDir, variant, HASH_TXT)
                        hashMap = CFixMapUtils.parseMap(hashFile)
                    }

                    def dirName = variant.dirName
                    cfixDir = new File("${project.buildDir}/outputs/cfix")
                    def outputDir = new File("${cfixDir}/${dirName}")
                    def hashFile = new File(outputDir, "hash.txt")

                    def cfixPatch = "cfix${variant.name.capitalize()}Patch"
                    project.task(cfixPatch) << {
                        if (patchDir) {
                            CFixAndroidUtils.dex(project, patchDir)
                        }
                    }
                    def cfixPatchTask = project.tasks[cfixPatch]

                    def cfixJarBeforeDex = "cfixJarBeforeDex${variant.name.capitalize()}"
                    project.task(cfixJarBeforeDex) << {
                        Set<File> inputFiles = transformClassesWithDexTask.inputs.files.files
                        inputFiles.each { file ->
                            println("> cfix: input: ${file.absolutePath}")
                        }
                        Set<File> singleFiles = new HashSet();
                        CFixFileUtils.getSingleFiles(inputFiles, singleFiles);
                        singleFiles.each { file ->
                            def path = file.absolutePath
                            if (path.endsWith(".jar")) {
                                CFixProcessor.processJar(file, hashFile, hashMap, patchDir, includePackage, excludeClass)
                            } else if (path.endsWith(".class")) {
                                CFixProcessor.processClass(variant, file, hashFile, hashMap, patchDir, includePackage, excludeClass)
                            }
                        }
                    }
                    cfixJarBeforeDexTask = project.tasks[cfixJarBeforeDex]
                    cfixJarBeforeDexTask.dependsOn transformClassesWithDexTask.taskDependencies.getDependencies(transformClassesWithDexTask)
                    transformClassesWithDexTask.dependsOn cfixJarBeforeDexTask

                    cfixJarBeforeDexTask.doFirst {
                        def applicationName = CFixAndroidUtils.getApplication(manifestFile)
                        if (applicationName != null) {
                            excludeClass.add(applicationName)
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
                            def mapFile = new File("${project.buildDir}/outputs/mapping/${variant.dirName}/mapping.txt")
                            def newMapFile = new File("${cfixDir}/${variant.dirName}/mapping.txt");
                            FileUtils.copyFile(mapFile, newMapFile)
                        }
                    }

                    cfixPatchTask.dependsOn cfixJarBeforeDexTask
                }
            }

            project.task(CFIX_PATCHES) << {
                patchList.each { patchDir ->
                    CFixAndroidUtils.dex(project, patchDir)
                }
            }

            project.tasks[CFIX_PATCHES].dependsOn cfixJarBeforeDexTask
        }
    }
}
