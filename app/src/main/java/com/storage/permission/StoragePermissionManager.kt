package com.storage.permission

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import com.target.cleaner.base.permission.PA
import com.target.cleaner.base.permission.PAR

/* SD卡权限工具 */
object StoragePermissionManager {

    // 是否请求SAF管理權限
    const val REQUEST_SAF = true


    private const val path = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata/document/primary%3A%2FAndroid%2Fdata"
    val androidDataUri: Uri = Uri.parse(path)


    /*
    检测权限
     */
    @SuppressLint("NewApi")
    fun checkPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < 30)
            return ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        if (!REQUEST_SAF)
            return Environment.isExternalStorageManager()
        return Environment.isExternalStorageManager() &&
                DocumentFile.fromTreeUri(activity, androidDataUri)?.canRead() == true
    }

    /*
    请求权限
     */
    fun requestPermission(activity: Activity, result: (grant: Boolean) -> Unit) {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (checkPermission(activity)) {
            result.invoke(true)
        } else {
            if (Build.VERSION.SDK_INT < 30) PA.launch(activity, permission, result)
            else PAR.launch(activity, result)
        }
    }


}