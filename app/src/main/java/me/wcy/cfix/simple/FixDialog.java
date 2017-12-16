package me.wcy.cfix.simple;

import android.content.Context;
import android.support.v7.app.AlertDialog;

/**
 * Created by wcy on 2017/12/16.
 */
public class FixDialog {
    private static FixDialog fixDialog;
    private Context context;

    public static FixDialog get(Context context) {
        if (fixDialog == null) {
            fixDialog = new FixDialog(context);
        }
        return fixDialog;
    }

    private FixDialog(Context context) {
        this.context = context;
    }

    public void show() {
        new AlertDialog.Builder(context)
                .setTitle("Congratulations")
                .setMessage("Patch Success!")
                .setPositiveButton("OK", null)
                .show();
    }
}
