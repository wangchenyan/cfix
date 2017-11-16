package me.wcy.cfix.simple;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import me.wcy.cfix.lib.CFix;

/**
 * Created by hzwangchenyan on 2017/11/14.
 */
public class CFixApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        CFix.init(this);
        CFix.loadPatch(this, Environment.getExternalStorageDirectory().getPath().concat("/patch.jar"));
    }
}
