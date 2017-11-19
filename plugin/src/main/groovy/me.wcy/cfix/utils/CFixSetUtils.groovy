package me.wcy.cfix.utils

class CFixSetUtils {

    static boolean isExcluded(String path, Set<String> excludeClass) {
        def isExcluded = false
        excludeClass.each { exclude ->
            if (path.endsWith(exclude)) {
                isExcluded = true
            }
        }
        return isExcluded
    }

    static boolean isIncluded(String path, Set<String> includePackage) {
        if (includePackage.size() == 0) {
            return true
        }

        def isIncluded = false
        includePackage.each { include ->
            if (path.contains(include)) {
                isIncluded = true
            }
        }
        return isIncluded
    }
}
