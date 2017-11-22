package me.wcy.cfix.lib;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import me.wcy.cfix.lib.utils.AssetUtils;
import me.wcy.cfix.lib.utils.DexUtils;
import me.wcy.cfix.lib.utils.SignChecker;

public class CFix {
    private static final String TAG = "CFix";
    private static final String HACK_DEX = "hack.apk";

    private static final String DEX_DIR = "cfix";
    private static final String DEX_OPT_DIR = "cfixopt";

    private static Context sContext;
    private static SignChecker sSignChecker;

    public static void init(Context context) {
        sContext = context;
        sSignChecker = new SignChecker(context);
        File dexDir = new File(context.getFilesDir(), DEX_DIR);
        dexDir.mkdir();

        String dexPath = null;
        try {
            dexPath = AssetUtils.copyAsset(context, HACK_DEX, dexDir);
        } catch (IOException e) {
            Log.e(TAG, "copy " + HACK_DEX + " failed");
            e.printStackTrace();
        }

        loadPatch(dexPath, false);
    }

    public static void loadPatch(String dexPath, boolean verify) {
        if (sContext == null) {
            Log.e(TAG, "context is null");
            return;
        }

        if (!new File(dexPath).exists()) {
            Log.e(TAG, dexPath + " is null");
            return;
        }

        if (verify && !sSignChecker.verifySign(dexPath)) {
            Log.e(TAG, "patch verify failed");
            return;
        }

        File dexOptDir = new File(sContext.getFilesDir(), DEX_OPT_DIR);
        dexOptDir.mkdir();
        try {
            DexUtils.injectDexAtFirst(dexPath, dexOptDir.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "inject " + dexPath + " failed");
            e.printStackTrace();
        }
    }
}
