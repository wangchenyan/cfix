package me.wcy.cfix

import org.gradle.api.Project

class CFixExtension {
    HashSet<String> includePackage = []
    HashSet<String> excludeClass = []
    boolean debugOn = true

    boolean sign = false
    File storeFile = null
    String storePassword = ''
    String keyAlias = ''
    String keyPassword = ''

    CFixExtension(Project project) {
    }
}
