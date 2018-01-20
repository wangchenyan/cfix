package me.wcy.cfix.utils

import com.android.build.gradle.api.BaseVariant

class CFixLogger {
    private static String TAG

    static void init(BaseVariant variant) {
        TAG = "> CFix-${variant.name.capitalize()}"
    }

    static void i(String log) {
        println("${TAG}: ${log}")
    }
}