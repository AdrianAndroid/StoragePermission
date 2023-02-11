package com.target.cleaner.base.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.storage.permission.StoragePermissionManager
import kotlinx.coroutines.*
import java.util.*


@RequiresApi(Build.VERSION_CODES.R)
class PAR : AppCompatActivity() {

    companion object {
        private val callbacks = HashMap<String, (Boolean) -> Unit>()
        fun launch(activity: Activity, callback: (Boolean) -> Unit) {
            val callbackID = UUID.randomUUID().toString()
            callbacks[callbackID] = callback
            activity.startActivity(Intent(activity, PAR::class.java).apply {
                putExtra("id", callbackID)
            })
        }
    }


    private val codeDocument = 7778
    private val id by lazy { intent.getStringExtra("id") ?: "" }
    private var waitingForManagerPermissionOK = false
    private val scope = MainScope()
    private var taskStarted = false


    private fun startTaskOnce() {
        if (taskStarted) return
        taskStarted = true
        when {
            id.isBlank() -> finish()
            Environment.isExternalStorageManager() -> requestAndroidDataPackage()
            else -> requestStoreManager()
        }
    }

    private fun requestStoreManager() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:${packageName}")
        try {
            // 极少部分神仙手机没有这个action, 所以start会报错
            startActivity(intent)
            waitingForManagerPermissionOK = true
            startListenManagerPermit()
        } catch (e: Exception) {
            callbacks[id]?.invoke(false)
            finish()
        }
    }

    private fun requestAndroidDataPackage() {
        if (!StoragePermissionManager.REQUEST_SAF) {
            callbacks[id]?.invoke(true)
            finish()
            return
        }
        kotlin.runCatching {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            intent.putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                StoragePermissionManager.androidDataUri
            )
            startActivityForResult(intent, codeDocument)
        }.onFailure {
            callbacks[id]?.invoke(false)
            finish()
        }
    }


    private fun startListenManagerPermit() = scope.launch {
        kotlin.runCatching {
            repeat(Int.MAX_VALUE) {
                delay(700)
                if (Environment.isExternalStorageManager()) {
                    this@PAR.startActivity(Intent(this@PAR, PAR::class.java))
                    cancel()
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (waitingForManagerPermissionOK) {
            waitingForManagerPermissionOK = false
            scope.cancel()
            if (StoragePermissionManager.REQUEST_SAF) {
                if (Environment.isExternalStorageManager()) {
                    requestAndroidDataPackage()
                } else {
                    callbacks[id]?.invoke(false)
                    finish()
                }
            } else {
                callbacks[id]?.invoke(Environment.isExternalStorageManager())
                finish()
            }
        }
        startTaskOnce()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            codeDocument -> {
                takePersistableUriPermission(data)
                if (StoragePermissionManager.checkPermission(this)) callbacks[id]?.invoke(true)
                else callbacks[id]?.invoke(false)
                finish()
            }
        }
    }

    private fun takePersistableUriPermission(data: Intent?) {
        data?.data?.run {
            val flag =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            kotlin.runCatching { contentResolver.takePersistableUriPermission(this, flag) }
        }
    }


    override fun onBackPressed() = Unit

    override fun onDestroy() {
        callbacks.remove(id)
        scope.cancel()
        super.onDestroy()
    }

}