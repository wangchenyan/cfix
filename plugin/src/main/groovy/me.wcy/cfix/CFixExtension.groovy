package me.wcy.cfix

import org.gradle.api.Project

/**
 * Created by jixin.jia on 15/11/4.
 */
class CFixExtension {
    HashSet<String> includePackage = []
    HashSet<String> excludeClass = []
    boolean debugOn = true

    CFixExtension(Project project) {
    }
}
