package me.wcy.cfix.sample;

import android.content.Context;
import android.support.v7.app.AlertDialog;

/**
 * Created by wcy on 2017/12/16.
 */
public class FixDialog {
    private static FixDialog fixDialog;

    public static FixDialog get() {
        if (fixDialog == null) {
            fixDialog = new FixDialog();
        }
        return fixDialog;
    }

    private FixDialog() {
    }

    public void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Congratulations")
                .setMessage("Patch Success!")
                .setPositiveButton("OK", null)
                .show();
    }
}
