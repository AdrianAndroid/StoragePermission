package com.storage.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.checkPermission).setOnClickListener {
            if (checkCleanPermission()) {
                Toast.makeText(this@MainActivity, "已经有权限", Toast.LENGTH_SHORT).show()
            } else if (checkDenyForeverAndToast(this@MainActivity)){
                Toast.makeText(this@MainActivity, "已经永久拒绝", Toast.LENGTH_SHORT).show()
            } else {
                StoragePermissionManager.requestPermission(this@MainActivity) { granted ->
                    Toast.makeText(this@MainActivity,
                        "StoragePermissionManager回调回来了! granted->$granted",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /*
     *   检测清理权限
     */
    private fun checkCleanPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= 30 -> {
                Environment.isExternalStorageManager()
            }
            else -> {
                ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    val isSystemOSUnder11 by lazy { Build.VERSION.SDK_INT < 30 }

    /* 判断是不是永远拒绝了权限, 如果从未请求过权限则无效 */
    fun checkDenyForeverAndToast(activity: Activity): Boolean {
        if (isSystemOSUnder11)
            return false
        val forever = checkDenyForeverOnLastCleanPermissionCallback(activity)
        if (forever) {
            Toast.makeText(this,
                "Please open the permission in system setting page.",
                Toast.LENGTH_SHORT).show()
            launchSystemAppSetting(packageName)
            return true
        }
        return false
    }

    /*
     * 检测清理权限请求结果回调后, 用户是否勾选了"拒绝并不再弹出"
     */
    fun checkDenyForeverOnLastCleanPermissionCallback(activity: Activity): Boolean {
        if (!isSystemOSUnder11 || checkCleanPermission()) return false
        val p = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
        return !showRationale
    }

    /* 打开系统app设置页面 */
    private fun launchSystemAppSetting(packageName: String) {
        runCatching {
            startActivity(Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = Uri.fromParts("package", packageName, null)
            })
        }
    }
}