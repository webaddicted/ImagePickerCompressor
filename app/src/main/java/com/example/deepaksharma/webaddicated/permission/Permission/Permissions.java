package com.example.deepaksharma.webaddicated.permission.Permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Permissions {
    private static final int PERMISSION_CODE = 1212;
    private static Activity mActivity;
    private static List<String> mCustomPermission = null;
    private static PermissionListener mPerpermissionListener;

    /**
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     * @param activity           referance of activity
     * @param permissionListener is describe permission status
     * @param permissions        is bundle of all permission
     */
    public static boolean checkAndRequestPermission(@NonNull Activity activity, @NonNull List<String> permissions, @NonNull PermissionListener permissionListener) {
        mActivity = activity;
        mPerpermissionListener = permissionListener;
        mCustomPermission = permissions;
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> listPermissionsNeeded = permissions;
            List<String> listPermissionsAssign = new ArrayList<>();
            for (String per : listPermissionsNeeded) {
                if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), per) != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsAssign.add(per);
                }
            }
            if (!listPermissionsAssign.isEmpty()) {
                ActivityCompat.requestPermissions(activity, listPermissionsAssign.toArray(new String[listPermissionsAssign.size()]), PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }

    /**
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     *
     * @param activity referance of activity
     * @param permissionListener is describe permission status
     * @param permissions is single permission
     */
    public static boolean checkAndRequestPermission(@NonNull Activity activity, @NonNull String permissions, @NonNull PermissionListener permissionListener) {
        mActivity = activity;
        mPerpermissionListener = permissionListener;
        mCustomPermission = Arrays.asList(new String[]{permissions});
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(activity, permissions) != PackageManager.PERMISSION_GRANTED) {
//                askRequestPermissions(new String[]{permissions});
                ActivityCompat.requestPermissions(activity, new String[]{permissions}, PERMISSION_CODE);
                return false;
            } else {
                // Permission already granted.
//                mPerpermissionListener.onPermissionGranted();
            }
        }
        return true;
    }

    public static void checkResult(@NonNull int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE: {
                List<String> listPermissionsNeeded = mCustomPermission;
                Map<String, Integer> perms = new HashMap<>();
                for (String permission : listPermissionsNeeded) {
                    perms.put(permission, PackageManager.PERMISSION_GRANTED);
                }
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    boolean isAllGranted = true;
                    for (String permission : listPermissionsNeeded) {
                        if (perms.get(permission) == PackageManager.PERMISSION_DENIED) {
                            isAllGranted = false;
                            break;
                        }
                    }
                    if (isAllGranted) {
                        mPerpermissionListener.onPermissionGranted(mCustomPermission);
                    } else {
                        boolean shouldRequest = false;
                        for (String permission : listPermissionsNeeded) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) {
                                shouldRequest = true;
                                break;
                            }
                        }
                        if (shouldRequest) {
                            ifCancelledAndCanRequest(mActivity);
                        } else {
                            //permission is denied (and never ask again is  checked)
                            //shouldShowRequestPermissionRationale will return false
                            ifCancelledAndCannotRequest(mActivity);
                        }
                    }
                }
            }
        }
    }

    /**
     * permission cancel dialog
     *
     * @param activity
     */
    private static void ifCancelledAndCanRequest(final Activity activity) {
        showDialogOK(activity, "Permission required for this app, please grant all permission .", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        checkAndRequestPermission(activity, mCustomPermission, mPerpermissionListener);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mPerpermissionListener.onPermissionDenied(mCustomPermission);
                        // proceed with logic by disabling the related features or quit the app.
                        break;
                }
            }
        });
    }

    /**
     * forcefully stoped all permission dialog
     *
     * @param activity
     */
    private static void ifCancelledAndCannotRequest(final Activity activity) {
        showDialogOK(activity, "You have forcefully denied some of the required permissions \n" + "for this action. Please go to permissions and allow them.", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivity(intent);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mPerpermissionListener.onPermissionDenied(mCustomPermission);
                        // proceed with logic by disabling the related features or quit the app.
                        break;
                }
            }
        });
    }

    private static void showDialogOK(Activity activity, String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(activity).setMessage(message).setPositiveButton("OK", okListener).setNegativeButton("Cancel", okListener).create().show();
    }
    /**
     * Check if version is marshmallow and above.
     * @param activity           referance of activity
     * @param permissionListener is describe permission status
     * @param permissions        is bundle of all permission
     */
    public static boolean checkPermission(@NonNull Activity activity, @NonNull PermissionListener permissionListener, @NonNull List<String> permissions) {
        mActivity = activity;
        mPerpermissionListener = permissionListener;
        mCustomPermission = permissions;
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> listPermissionsNeeded = permissions;
            List<String> listPermissionsAssign = new ArrayList<>();
            for (String per : listPermissionsNeeded) {
                if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), per) != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsAssign.add(per);
                }
            }
            if (!listPermissionsAssign.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}

