package com.dullyoung.utils.helper;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;


import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    public static final int REQUEST_MANAGE_ALL_FILE_CODE = 1025;
    public static final int REQUEST_NORMAL_PERMISSION_CODE = 1024;

    private String[] mustPermissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};

    public void setMustPermissions(String[] mustPermissions) {
        this.mustPermissions = mustPermissions;
    }

    public void setMustPermissions2(String... mustPermissions) {
        this.mustPermissions = mustPermissions;
    }

    public PermissionHelper justStoragePermission() {
        this.mustPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION};
        return this;
    }

    private boolean checkManagerExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return true;
        }
    }


    public void checkAndRequestManagerExternalStoragePermission(Activity activity, OnRequestPermissionsCallback callback) {
        this.onRequestPermissionsCallback = callback;
        if (checkManagerExternalStoragePermission()) {
            this.onRequestPermissionsCallback.onRequestPermissionSuccess();
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            activity.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILE_CODE);
        }
    }

    public void checkAndRequestPermission(Activity activity, OnRequestPermissionsCallback onRequestPermissionsCallback) {
        this.onRequestPermissionsCallback = onRequestPermissionsCallback;

        if (Build.VERSION.SDK_INT < 23) {
            if (onRequestPermissionsCallback != null) {
                onRequestPermissionsCallback.onRequestPermissionSuccess();
            }
            return;
        }

        if (checkMustPermissions(activity)) {
            if (onRequestPermissionsCallback != null) {
                onRequestPermissionsCallback.onRequestPermissionSuccess();
            }
            return;
        }
        ActivityCompat.requestPermissions(activity, mustPermissions, REQUEST_NORMAL_PERMISSION_CODE);
    }

    @TargetApi(23)
    public boolean checkMustPermissions(Activity activity) {
        if (mustPermissions == null || mustPermissions.length == 0) {
            return true;
        }
        List<String> lackedPermission = new ArrayList<>();
        for (String permissions : mustPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permissions) != PackageManager.PERMISSION_GRANTED) {
                lackedPermission.add(permissions);
            }
        }

        return 0 == lackedPermission.size();
    }

    /**
     * {@link androidx.appcompat.app.AppCompatActivity#startActivityForResult(Intent, int)}这个方法会回调两次
     * 用 callBackTime 来做限制
     */
    private long callBackTime = 0;

    public void onRequestPermissionsResult(Activity activity, int requestCode) {
        switch (requestCode) {
            case REQUEST_NORMAL_PERMISSION_CODE:
                if (checkMustPermissions(activity)) {
                    if (onRequestPermissionsCallback != null) {
                        onRequestPermissionsCallback.onRequestPermissionSuccess();
                    }
                } else {
                    if (onRequestPermissionsCallback != null) {
                        onRequestPermissionsCallback.onRequestPermissionError();
                    }
                }
                break;
            case REQUEST_MANAGE_ALL_FILE_CODE:
                if (System.currentTimeMillis() - callBackTime < 1000) {
                    return;
                }
                callBackTime = System.currentTimeMillis();
                if (checkManagerExternalStoragePermission()) {
                    if (onRequestPermissionsCallback != null) {
                        onRequestPermissionsCallback.onRequestPermissionSuccess();
                    }
                } else {
                    if (onRequestPermissionsCallback != null) {
                        onRequestPermissionsCallback.onRequestPermissionError();
                    }
                }
                break;
            default:
                break;
        }


    }

    private OnRequestPermissionsCallback onRequestPermissionsCallback;
    public interface OnRequestPermissionsCallback {
        void onRequestPermissionSuccess();
        void onRequestPermissionError();
    }
}
