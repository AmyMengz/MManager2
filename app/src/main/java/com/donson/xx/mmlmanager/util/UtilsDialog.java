package com.donson.xx.mmlmanager.util;

import android.content.Context;

import com.afollestad.materialdialogs.MaterialDialog;

/**
 * Created by Administrator on 2017/5/11.
 */

public class UtilsDialog {
    public static MaterialDialog showTitleContentWithProgress(Context context, String title, String content) {
        MaterialDialog.Builder materialBuilder = new MaterialDialog.Builder(context)
                .title(title)
                .content(content)
                .cancelable(false)
                .progress(true, 0);
        return materialBuilder.show();
    }

    public static void showTitleContent(Context context, String string, String string1) {
    }
//    public static SnackBar showSnackbar(Activity activity, String text, @Nullable String buttonText, @Nullable final File file, Integer style) {
//
//    }
}
