package com.example.smsreader

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SmsNotificationListener : NotificationListenerService() {

    private lateinit var db: SmsDbHelper
    private lateinit var embedder: BertEmbedder

    override fun onCreate() {
        super.onCreate()
        db = SmsDbHelper(this)
        embedder = BertEmbedder(this)
        if (embedder.isReady) {
            val kwMap = db.queryCategoryKeywords()
            MessageClassifier.initPrototypes({ text -> embedder.embed(text) }, kwMap)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val pkg = sbn.packageName.lowercase()
        if (!pkg.contains("mms") && !pkg.contains("sms") && !pkg.contains("messaging")) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (body.isEmpty() && title.isEmpty()) return

        if (MessageClassifier.isBlocked(title, body)) return

        val smsTime = MessageClassifier.extractTime("$title $body") ?: System.currentTimeMillis()
        db.insertSms(body, smsTime)

        // 资金短信 → 提取银行+金额 → 嵌入分类 → 存入bill
        MessageClassifier.extractBill(title, body)?.let { bill ->
            val result = MessageClassifier.classifyBill(
                embed = { embedder.embed(it) },
                rawText = bill.rawText,
                amount = bill.amount
            )
            db.insertBill(bill.amount, bill.bankName, result.category, smsTime, result.direction)
        } ?: MessageClassifier.extractPackage(title, body)?.let { pkg ->
            // 包裹短信 → 提取公司+取件码 → 嵌入推断地址
            val vec = embedder.embed(pkg.rawText)
            // TODO: vec → 地址提取模型 → address
            val address = ""
            db.insertPackage(pkg.company, address, smsTime, pkg.pickupCode, "运输中", pkg.rawText)
        }

        // 事件：文本时间超过短信发送时间1天
        MessageClassifier.extractEventDate(title, body)?.let { eventDate ->
            if (eventDate - smsTime > 86400000) {
                db.insertEvent(eventDate, body)
            }
        }
    }

    override fun onListenerConnected() {}
    override fun onDestroy() {
        super.onDestroy()
        if (::embedder.isInitialized) embedder.close()
    }
}
