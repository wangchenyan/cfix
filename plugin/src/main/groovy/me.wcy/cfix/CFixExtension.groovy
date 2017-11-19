package me.wcy.cfix

import org.gradle.api.Project

class CFixExtension {
    HashSet<String> includePackage = []
    HashSet<String> excludeClass = []
    boolean debugOn = true

    CFixExtension(Project project) {
    }
}
