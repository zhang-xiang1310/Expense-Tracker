package com.example.smsreader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Telephony
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var totalText: TextView
    private lateinit var db: SmsDbHelper
    private lateinit var eventRows: LinearLayout
    private lateinit var pkgRows: LinearLayout
    private lateinit var smsRows: LinearLayout
    private val fmt = SimpleDateFormat("M月d日", Locale.CHINA)

    private val smsPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) readSmsFromInbox() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(createHomePage())
    }

    override fun onResume() {
        super.onResume()
        reloadFundsCard()
    }

    private fun reloadFundsCard() {
        val cards = db.queryAllBankCards()
        val total = cards.sumOf { it.balance }
        totalText.text = String.format("¥ %,.2f", total)
        val cardCount = cards.size
        // update subtitle
        (totalText.parent as? android.view.ViewGroup)?.let { card ->
            for (i in 0 until card.childCount) {
                val child = card.getChildAt(i)
                if (child is android.widget.TextView && child.text.toString().contains("张银行卡")) {
                    child.text = "${cardCount}张银行卡 · 查看详情  >"
                }
            }
        }
    }

    // ═══════════════════════════════════════════
    // 主页布局
    // ═══════════════════════════════════════════

    private fun createHomePage(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }

        // 标题栏
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(TextView(this@MainActivity).apply {
                text = "大傻春"
                textSize = 22f
                setTextColor(0xFF212121.toInt())
                setTypeface(null, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, -2, 1f))

            addView(TextView(this@MainActivity).apply {
                text = "收支  >"
                textSize = 13f
                setTextColor(0xFF1976D2.toInt())
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, SpendingDetailActivity::class.java))
                }
            })
        })
        list.addView(spacer(0, dp(16)))

        // 总资金卡片
        list.addView(fundsCard())
        list.addView(spacer(0, dp(20)))

        // 短信列表
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(sectionTitle("短信"), LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "读取短信"
                textSize = 13f
                setTextColor(0xFF1976D2.toInt())
                setPadding(dp(8), dp(4), dp(4), dp(4))
                setOnClickListener {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_SMS)
                        == PackageManager.PERMISSION_GRANTED) {
                        readSmsFromInbox()
                    } else {
                        smsPermLauncher.launch(Manifest.permission.READ_SMS)
                    }
                }
            })
        })
        smsRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(smsRows)
        buildSmsRows()

        list.addView(spacer(0, dp(20)))

        // 事件提醒列表
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(sectionTitle("事件提醒"), LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "查看全部  >"
                textSize = 13f
                setTextColor(0xFF1976D2.toInt())
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, EventDetailActivity::class.java))
                }
            })
        })
        eventRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(eventRows)
        buildEventRows()

        list.addView(spacer(0, dp(20)))

        // 包裹提醒列表
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(sectionTitle("包裹提醒"), LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@MainActivity).apply {
                text = "查看全部  >"
                textSize = 13f
                setTextColor(0xFF1976D2.toInt())
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, PackageDetailActivity::class.java))
                }
            })
        })
        pkgRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(pkgRows)
        buildPkgRows()

        return ScrollView(this).apply { addView(list) }
    }

    private fun fundsCard(): LinearLayout {
        val cards = db.queryAllBankCards()
        val total = cards.sumOf { it.balance }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = roundRect(0xFF2196F3.toInt(), dp(16))
            elevation = dp(3).toFloat()
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BankCardActivity::class.java))
            }

            addView(TextView(this@MainActivity).apply {
                text = "总资金"
                textSize = 14f
                setTextColor(0xCCFFFFFF.toInt())
            })
            totalText = TextView(this@MainActivity).apply {
                text = String.format("¥ %,.2f", total)
                textSize = 34f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(6), 0, 0)
            }
            addView(totalText)
            addView(TextView(this@MainActivity).apply {
                text = "${cards.size}张银行卡 · 查看详情  >"
                textSize = 13f
                setTextColor(0x99FFFFFF.toInt())
                setPadding(0, dp(12), 0, 0)
            })
        }
    }

    // ═══════════════════════════════════════════
    // 事件 & 包裹列表项
    // ═══════════════════════════════════════════

    private fun eventRow(name: String, date: String, desc: String, color: String): LinearLayout {
        val dotColor = when (color) {
            "red" -> 0xFFE91E63.toInt()
            "blue" -> 0xFF2196F3.toInt()
            "orange" -> 0xFFFF9800.toInt()
            else -> 0xFF607D8B.toInt()
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundRect(0xFFFFFFFF.toInt(), dp(8))
            elevation = dp(1).toFloat()

            addView(dot(dotColor))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(TextView(this@MainActivity).apply {
                    text = name
                    textSize = 15f
                    setTextColor(0xFF212121.toInt())
                })
                addView(TextView(this@MainActivity).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(0xFF9E9E9E.toInt())
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@MainActivity).apply {
                text = date
                textSize = 14f
                setTextColor(dotColor)
                setTypeface(null, Typeface.BOLD)
            })
        }
    }

    private fun packageRow(name: String, status: String, desc: String, color: String): LinearLayout {
        val dotColor = when (color) {
            "green" -> 0xFF4CAF50.toInt()
            "blue" -> 0xFF2196F3.toInt()
            "orange" -> 0xFFFF9800.toInt()
            else -> 0xFFBDBDBD.toInt()
        }
        val statusColor = if (color == "gray") 0xFF9E9E9E.toInt() else 0xFF212121.toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundRect(0xFFFFFFFF.toInt(), dp(8))
            elevation = dp(1).toFloat()

            addView(dot(dotColor))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(TextView(this@MainActivity).apply {
                    text = name
                    textSize = 15f
                    setTextColor(0xFF212121.toInt())
                })
                addView(TextView(this@MainActivity).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(0xFF9E9E9E.toInt())
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@MainActivity).apply {
                text = status
                textSize = 13f
                setTextColor(statusColor)
            })
        }
    }

    // ═══════════════════════════════════════════
    // DB 列表构建
    // ═══════════════════════════════════════════

    private fun readSmsFromInbox() {
        val since = db.getLatestSmsDate()
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val proj = arrayOf("body", "date")
        val sel = if (since > 0) "date > ?" else null
        val selArgs = if (since > 0) arrayOf(since.toString()) else null

        contentResolver.query(uri, proj, sel, selArgs, "date ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                val body = cursor.getString(0) ?: continue
                val date = cursor.getLong(1)
                if (body.isEmpty() || db.smsExists(body, date)) continue
                db.insertSms(body, date)
                classifySms(body, date)
            }
        }
        buildSmsRows()
    }

    /** 对单条短信做分类入库，不依赖 BERT 模型 */
    private fun classifySms(body: String, date: Long) {
        if (MessageClassifier.isBlocked("", body)) return

        MessageClassifier.extractBill("", body)?.let { bill ->
            val result = MessageClassifier.classifyBillByKeywords(bill.rawText, bill.amount)
            db.insertBill(bill.amount, bill.bankName, result.category, date, result.direction)
            return
        }
        MessageClassifier.extractPackage("", body)?.let { pkg ->
            db.insertPackage(pkg.company, "", date, pkg.pickupCode, "运输中", body)
            return
        }
        MessageClassifier.extractEventDate("", body)?.let { eventDate ->
            if (eventDate - date > 86400000) {
                db.insertEvent(eventDate, body)
            }
        }
    }

    private fun buildSmsRows() {
        smsRows.removeAllViews()
        val list = db.queryAllSms()
        if (list.isEmpty()) {
            smsRows.addView(TextView(this).apply {
                text = "暂无短信，点击\"读取短信\"从收件箱导入"
                textSize = 12f
                setTextColor(0xFF9E9E9E.toInt())
                setPadding(dp(8), dp(4), 0, 0)
            })
            return
        }
        smsRows.addView(spacer(0, dp(4)))
        list.forEach { sms ->
            smsRows.addView(smsRow(sms), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }

    private fun smsRow(sms: Sms): LinearLayout {
        val dateStr = fmt.format(Date(sms.date))
        val preview = sms.body.replace("\n", " ").take(60)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = roundRect(0xFFFFFFFF.toInt(), dp(8))
            elevation = dp(1).toFloat()
            addView(TextView(this@MainActivity).apply {
                text = preview
                textSize = 14f
                setTextColor(0xFF212121.toInt())
                maxLines = 2
            })
            addView(TextView(this@MainActivity).apply {
                text = dateStr
                textSize = 11f
                setTextColor(0xFF9E9E9E.toInt())
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun eventColor(dueDate: Long): String {
        if (dueDate == 0L) return "blue"
        val days = (dueDate - System.currentTimeMillis()) / 86_400_000
        return when { days <= 3 -> "red"; days <= 7 -> "orange"; else -> "blue" }
    }

    private fun pkgColor(status: String): String {
        return when {
            status.contains("签收") -> "gray"
            status.contains("派送") -> "green"
            status.contains("运输") -> "blue"
            else -> "orange"
        }
    }

    private fun buildEventRows() {
        eventRows.removeAllViews()
        eventRows.addView(spacer(0, dp(8)))
        db.queryAllEvents().forEach { e ->
            val dateStr = if (e.date > 0) fmt.format(Date(e.date)) else "待定"
            val row = eventRow("事件", dateStr, e.body.take(20), eventColor(e.date))
            row.setOnClickListener { showEditEventDialog(e) }
            val swiped = swipeWrap(row) { db.deleteEvent(e.id); buildEventRows() }
            eventRows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }

    private fun showEditEventDialog(e: Event) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), 0)
        }
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val dateEdit = android.widget.EditText(this).apply {
            hint = "日期（yyyy-MM-dd）"
            setText(if (e.date > 0) dateFmt.format(Date(e.date)) else "")
        }
        val bodyEdit = android.widget.EditText(this).apply {
            hint = "事件内容"
            setText(e.body)
        }
        layout.addView(dateEdit)
        layout.addView(bodyEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        android.app.AlertDialog.Builder(this)
            .setTitle("编辑事件")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newDate = try { dateFmt.parse(dateEdit.text.toString().trim())?.time ?: 0L } catch (_: Exception) { 0L }
                val newBody = bodyEdit.text.toString().trim()
                if (newBody.isNotEmpty()) {
                    db.updateEvent(e.id, newDate, newBody)
                    buildEventRows()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildPkgRows() {
        pkgRows.removeAllViews()
        pkgRows.addView(spacer(0, dp(8)))
        db.queryAllPackages().forEach { p ->
            val status = p.status.ifEmpty { "处理中" }
            val desc = p.description.ifEmpty { p.address.ifEmpty { "暂无信息" } }
            val row = packageRow(p.company, status, desc, pkgColor(status))
            val swiped = swipeWrap(row) { db.deletePackage(p.id); buildPkgRows() }
            pkgRows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }
}
