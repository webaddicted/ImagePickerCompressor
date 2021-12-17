package com.webaddicted.imagepickercompressor.utils.common

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.webaddicted.imagepickercompressor.R
import java.util.*

/**
 * Created by Deepak Sharma(webaddicted) on 15/01/20.
 */
object PermissionHelper {
    private lateinit var mPermissionListener: (Boolean, List<String>) -> Unit
    private const val PERMISSION_CODE = 1212
    private var mCustomPermission: List<String>? = null

    fun requestSinglePermission(
        activity: Activity,
        permissions: String,
        permissionListener: (Boolean, List<String>) -> Unit
    ) {
        mPermissionListener = permissionListener
        mCustomPermission = listOf(permissions)
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    permissions
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //        askRequestPermissions(new String[]{permissions});
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permissions),
                    PERMISSION_CODE
                )
                return
            }
        }
        permissionListener(true, listOf(permissions))
    }

    /**
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     * NO of permission check a at time.
     *
     * @param permissionListener is describe permission status
     * @param permissions    is bundle of all permission
     */
    fun requestMultiplePermission(
        activity: Activity,
        permissions: List<String>,
        permissionListener: (Boolean, List<String>) -> Unit
    ) {
        mPermissionListener = permissionListener
        mCustomPermission = permissions
        if (Build.VERSION.SDK_INT >= 23) {
            val listPermissionsAssign = ArrayList<String>()
            for (per in permissions) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        per
                    ) != PackageManager.PERMISSION_GRANTED
                )
                    listPermissionsAssign.add(per)
            }
            if (listPermissionsAssign.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    listPermissionsAssign.toTypedArray(),
                    PERMISSION_CODE
                )
                return
            }
        }
        permissionListener(true, permissions)
    }

    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                val listPermissionsNeeded =
                    mCustomPermission
                val perms = HashMap<String, Int>()
                if (listPermissionsNeeded != null) {
                    for (permission in listPermissionsNeeded) {
                        perms[permission] = PackageManager.PERMISSION_GRANTED
                    }
                }
                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices)
                        perms[permissions[i]] = grantResults[i]
                    var isAllGranted = true
                    if (listPermissionsNeeded != null) {
                        for (permission in listPermissionsNeeded) {
                            if (perms[permission] == PackageManager.PERMISSION_DENIED) {
                                isAllGranted = false
                                break
                            }
                        }
                    }
                    if (isAllGranted) {
                        mCustomPermission?.let { mPermissionListener(true, it) }
                    } else {
                        var shouldRequest = false
                        if (listPermissionsNeeded != null) {
                            for (permission in listPermissionsNeeded) {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(
                                        activity,
                                        permission
                                    )
                                ) {
                                    shouldRequest = true
                                    break
                                }
                            }
                        }
                        if (shouldRequest) {
                            ifCancelledAndCanRequest(activity)
                        } else {
                            //permission is denied (and never ask again is checked)
                            //shouldShowRequestPermissionRationale will return false
                            ifCancelledAndCannotRequest(activity)
                        }
                    }
                }
            }
        }
    }

    /**
     * permission cancel dialog
     */
    private fun ifCancelledAndCanRequest(activity: Activity) {
        showDialogOK(
            activity, "Permission required for this app, please grant all permission."
        ) { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> mCustomPermission?.let { it1 ->
                    mPermissionListener?.let { it2 ->
                        requestMultiplePermission(
                            activity,
                            it1,
                            it2
                        )
                    }
                }
                DialogInterface.BUTTON_NEGATIVE -> mCustomPermission?.let { it1 ->
                    mPermissionListener(
                        false,
                        it1
                    )
                }
            }// proceed with logic by disabling the related features or quit the app.
        }
    }

    /**
     * forcefully stoped all permission dialog
     */
    private fun ifCancelledAndCannotRequest(activity: Activity) {
        showDialogOK(
            activity,
            activity.resources.getString(
                R.string.forcefully_enable_permission
            )
        ) { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        activity.packageName,
                        null
                    )
                    intent.data = uri
                    activity.startActivity(
                        intent
                    )
                }
                DialogInterface.BUTTON_NEGATIVE -> mCustomPermission?.let { it1 ->
                    mPermissionListener(
                        false,
                        it1
                    )
                }
            }// proceed with logic by disabling the related features or quit the app.
        }
    }

    private fun showDialogOK(
        activity: Activity,
        message: String,
        okListener: DialogInterface.OnClickListener
    ) {
        AlertDialog.Builder(activity).setMessage(message).setPositiveButton(
            activity.resources.getString(R.string.ok),
            okListener
        ).setNegativeButton(activity.resources.getString(R.string.cancel), okListener).create()
            .show()
    }

    fun checkMultiplePermission(activity: Activity, permissions: List<String>): Boolean {
        mCustomPermission = permissions
        if (Build.VERSION.SDK_INT >= 23) {
            val listPermissionsAssign = ArrayList<String>()
            for (per in permissions) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        per
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    listPermissionsAssign.add(per)
                }
            }
            if (listPermissionsAssign.isNotEmpty()) {
                return false
            }
        }
        return true
    }

    /**
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     * check single permission
     *
     * @param permissions is single permission
     */
    fun checkPermission(activity: Activity, permissions: String): Boolean {
        mCustomPermission = listOf(permissions)
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    permissions
                ) != PackageManager.PERMISSION_GRANTED
            )
                return false
        }
        return true
    }

    fun clearPermission() {
        mCustomPermission = null
    }
}