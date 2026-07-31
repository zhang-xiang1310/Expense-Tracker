package com.example.smsreader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            textSize = 14f
            setLineSpacing(8f, 1f)
        }
        scrollView.addView(textView)
        layout.addView(scrollView)
        setContentView(layout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_SMS),
                1
            )
            textView.text = "需要短信权限"
        } else {
            loadSms(textView)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val textView = (findViewById<ScrollView>(android.R.id.content)
                ?.getChildAt(0) as? TextView) ?: return
            loadSms(textView)
        }
    }

    private fun loadSms(textView: TextView) {
        try {
            val sb = StringBuilder()
            var count = 0
            contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf("address", "body", "date"),
                null, null,
                "date DESC"
            )?.use { cursor ->
                while (cursor.moveToNext() && count < 500) {
                    val addr = cursor.getString(0) ?: "-"
                    val body = cursor.getString(1) ?: ""
                    sb.append("$addr\n$body\n\n")
                    count++
                }
            }
            if (sb.isEmpty()) {
                textView.text = "暂无短信"
            } else {
                textView.text = "共 $count 条\n\n$sb"
            }
        } catch (e: Exception) {
            textView.text = "读取失败: ${e.message}"
            Toast.makeText(this, "错误: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
