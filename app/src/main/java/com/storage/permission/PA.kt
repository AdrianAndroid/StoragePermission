package com.target.cleaner.base.permission

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*
import kotlin.collections.HashMap

class PA : AppCompatActivity() {

    companion object {
        private val callbacks = HashMap<String, (Boolean) -> Unit>()
        fun launch(activity: Activity, p: String, callback: (Boolean) -> Unit) {
            val callbackID = UUID.randomUUID().toString()
            callbacks[callbackID] = callback
            activity.startActivity(Intent(activity, PA::class.java).apply {
                putExtra("p", p)
                putExtra("id", callbackID)
            })
        }
    }


    private val p by lazy { intent.getStringExtra("p") ?: "" }
    private val id by lazy { intent.getStringExtra("id") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (p.isBlank() || id.isBlank()) finish()
        else ActivityCompat.requestPermissions(this, arrayOf(p), 7777)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val resultOK =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        callbacks[id]?.invoke(resultOK)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        callbacks.remove(id)
    }


    override fun onBackPressed() = Unit

}