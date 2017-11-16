package me.wcy.cfix.lib;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import me.wcy.cfix.lib.util.AssetUtils;
import me.wcy.cfix.lib.util.DexUtils;

/**
 * Created by jixin.jia on 15/10/31.
 */
public class CFix {
    private static final String TAG = "cfix";
    private static final String HACK_DEX = "hack.apk";

    private static final String DEX_DIR = "cfix";
    private static final String DEX_OPT_DIR = "cfixopt";

    public static void init(Context context) {
        File dexDir = new File(context.getFilesDir(), DEX_DIR);
        dexDir.mkdir();

        String dexPath = null;
        try {
            dexPath = AssetUtils.copyAsset(context, HACK_DEX, dexDir);
        } catch (IOException e) {
            Log.e(TAG, "copy " + HACK_DEX + " failed");
            e.printStackTrace();
        }

        loadPatch(context, dexPath);
    }

    public static void loadPatch(Context context, String dexPath) {
        if (context == null) {
            Log.e(TAG, "context is null");
            return;
        }
        if (!new File(dexPath).exists()) {
            Log.e(TAG, dexPath + " is null");
            return;
        }
        File dexOptDir = new File(context.getFilesDir(), DEX_OPT_DIR);
        dexOptDir.mkdir();
        try {
            DexUtils.injectDexAtFirst(dexPath, dexOptDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "inject " + dexPath + " failed");
            e.printStackTrace();
        }
    }
}
