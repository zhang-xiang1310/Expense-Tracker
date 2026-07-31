package com.example.smsreader

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            textSize = 14f
            setLineSpacing(6f, 1f)
            setPadding(32, 32, 32, 32)
        }
        setContentView(ScrollView(this).apply { addView(textView) })

        if (!isNotificationListenerEnabled()) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            textView.text = "请开启通知读取权限\n\n设置 → 通知使用权 → 找到本应用 → 开启\n\n开启后返回即可"
        } else {
            val list = SmsNotificationListener.getSavedSms(this)
            if (list.isEmpty()) {
                textView.text = "等待短信...\n\n有新的短信通知时会自动显示"
            } else {
                textView.text = list.joinToString("\n\n")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNotificationListenerEnabled()) {
            val textView = (findViewById<ScrollView>(android.R.id.content)
                ?.getChildAt(0) as? TextView) ?: return
            val list = SmsNotificationListener.getSavedSms(this)
            if (list.isNotEmpty()) {
                textView.text = list.joinToString("\n\n")
            }
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val listeners = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return listeners.contains(packageName)
    }
}
