package com.example.smsreader

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private const val FILE_NAME = "sms_log.txt"

        fun getSavedSms(context: Context): List<String> {
            val file = File(context.filesDir, FILE_NAME)
            return if (file.exists()) file.readLines().reversed() else emptyList()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val pkg = sbn.packageName.lowercase()
        if (!pkg.contains("mms") && !pkg.contains("sms") && !pkg.contains("messaging")) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (text.isEmpty() && title.isEmpty()) return

        val time = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
        val line = "$time | $title | $text"

        val file = File(filesDir, FILE_NAME)
        file.appendText("$line\n")
    }

    override fun onListenerConnected() {}
}
