package com.example.smsreader.model

import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SmsItem(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            return sdf.format(Date(date))
        }

    val isReceived: Boolean
        get() = type == Telephony.Sms.MESSAGE_TYPE_INBOX

    val typeLabel: String
        get() = when (type) {
            Telephony.Sms.MESSAGE_TYPE_INBOX -> "收到"
            Telephony.Sms.MESSAGE_TYPE_SENT -> "已发"
            Telephony.Sms.MESSAGE_TYPE_DRAFT -> "草稿"
            else -> "其他"
        }
}
