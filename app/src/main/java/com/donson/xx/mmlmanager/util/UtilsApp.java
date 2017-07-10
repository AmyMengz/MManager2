package com.donson.xx.mmlmanager.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import com.donson.xx.mmlmanager.MLManagerApplication;
import com.donson.xx.mmlmanager.R;
import com.donson.xx.mmlmanager.beans.AppInfo;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Administrator on 2017/5/10.
 */

public class UtilsApp {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_READ = 1;

    public static File getDefaultAppFolder() {
        return new File(Environment.getExternalStorageDirectory() + "/MMManager");
    }

    public static Drawable getIconFromCache(Context context, AppInfo appInfo) {
        Drawable res;

        try {
            File fileUri = new File(context.getCacheDir(), appInfo.getAPK());
            Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath());
            res = new BitmapDrawable(context.getResources(), bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            res = context.getResources().getDrawable(R.drawable.ic_android);
        }

        return res;
    }

    public static boolean copyFile(AppInfo appInfo) {
        Boolean res = false;
        File initialFile = new File(appInfo.getSource());
        File finalFile = getOutputFilename(appInfo);
        try {
            FileUtils.copyFile(initialFile, finalFile);
            res = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;

    }


    private static String getApkFilename(AppInfo appInfo) {
        AppPreferences appPreferences = MLManagerApplication.getAppPreferences();
        String res;
        switch (appPreferences.getCustomFilename()) {
            case "1":
                res = appInfo.getAPK() + "_" + appInfo.getVersion();
                break;
            case "2":
                res = appInfo.getName() + "_" + appInfo.getVersion();
                break;
            case "4":
                res = appInfo.getName();
                break;
            default:
                res = appInfo.getAPK();
                break;
        }
        return res;
    }

    public static File getAppFolder() {
        AppPreferences appPreference = MLManagerApplication.getAppPreferences();
        return new File(appPreference.getCustomPath());

    }

    public static Intent getShareIntent(File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }


    public static File getOutputFilename(AppInfo appInfo) {
        return new File(getAppFolder().getPath() + File.separator + getApkFilename(appInfo) + ".apk");
    }


    public static boolean checkPermissions(Activity activity) {
        Boolean res = false;
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_READ
            );
        } else {
            res = true;
        }
        return res;
    }
    public static Boolean extractMLManagerPro(Context context, AppInfo appInfo) {
        Boolean res = false;
        File finalFile = new File(getAppFolder().getPath(),getAPKFilename(appInfo)+".png");
        try{
            File fileUri = new File(context.getCacheDir(),getApkFilename(appInfo)+".png");
            FileOutputStream out =new FileOutputStream(fileUri);
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.banner_troll);
            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
            FileUtils.moveFile(fileUri,finalFile);
            res = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    private static String getAPKFilename(AppInfo appInfo) {
        AppPreferences appPreferences = MLManagerApplication.getAppPreferences();
        String res;
        switch (appPreferences.getCustomFilename()){
            case "1":
                res = appInfo.getAPK()+"_"+appInfo.getVersion();
                break;
            case "2":
                res = appInfo.getName()+"_"+appInfo.getVersion();
                break;
            case "4":
                res = appInfo.getName();
                break;
            default:
                res = appInfo.getAPK();
                break;
        }
        return  res;
    }
}
