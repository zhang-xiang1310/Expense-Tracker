package com.example.smsreader

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var totalText: TextView
    private lateinit var cardSubtitle: TextView
    private lateinit var db: SmsDbHelper
    private lateinit var eventRows: LinearLayout
    private lateinit var pkgRows: LinearLayout
    private lateinit var smsRows: LinearLayout
    private var smsExpanded = true
    private var eventExpanded = true
    private var pkgExpanded = true
    private val fmt = SimpleDateFormat("M月d日", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(createHomePage())
    }

    override fun onResume() {
        super.onResume()
        reloadFundsCard()
        buildEventRows()
        buildPkgRows()
    }

    private fun reloadFundsCard() {
        val cards = db.queryAllBankCards()
        val total = cards.sumOf { it.balance }
        totalText.text = String.format("¥ %,.2f", total)
        cardSubtitle.text = "${cards.size}张银行卡 · 查看详情  >"
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
        list.addView(TextView(this@MainActivity).apply {
            text = "大傻春"
            textSize = 22f
            setTextColor(0xFF212121.toInt())
            setTypeface(null, Typeface.BOLD)
        })
        list.addView(spacer(0, dp(16)))

        // 圆形头像
        list.addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.agent)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SpendingDetailActivity::class.java))
            }
        }, LinearLayout.LayoutParams(dp(80), dp(80)).apply { gravity = Gravity.CENTER })
        list.addView(spacer(0, dp(12)))

        // 余额
        val cards = db.queryAllBankCards()
        val total = cards.sumOf { it.balance }
        totalText = TextView(this@MainActivity).apply {
            text = String.format("¥ %,.2f", total)
            textSize = 28f
            setTextColor(0xFF212121.toInt())
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        list.addView(totalText)

        // 银行卡信息文字
        list.addView(TextView(this@MainActivity).apply {
            text = "${cards.size}张银行卡 · 查看详情  >"
            textSize = 13f
            setTextColor(0xFF1976D2.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BankCardActivity::class.java))
            }
        }.also { cardSubtitle = it })
        list.addView(spacer(0, dp(20)))

        // 快捷入口
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(TextView(this@MainActivity).apply {
                text = "词库管理  >"
                textSize = 12f; setTextColor(0xFF757575.toInt())
                setPadding(0, 0, dp(24), 0)
                setOnClickListener { startActivity(Intent(this@MainActivity, CategoryManageActivity::class.java)) }
            })
            addView(TextView(this@MainActivity).apply {
                text = "历史账单  >"
                textSize = 12f; setTextColor(0xFF757575.toInt())
                setOnClickListener { startActivity(Intent(this@MainActivity, BillHistoryActivity::class.java)) }
            })
        })

        // 短信列表
        val smsTitle = sectionTitle("短信 ▼")
        list.addView(smsTitle)
        smsRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(smsRows)
        buildSmsRows()
        smsTitle.setOnClickListener {
            smsExpanded = !smsExpanded
            smsTitle.text = if (smsExpanded) "短信 ▼" else "短信 ▶"
            smsRows.visibility = if (smsExpanded) android.view.View.VISIBLE else android.view.View.GONE
        }

        list.addView(spacer(0, dp(20)))

        // 事件提醒列表
        val eventTitle = sectionTitle("事件提醒 ▼")
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(eventTitle, LinearLayout.LayoutParams(0, -2, 1f))
        })
        eventRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(eventRows)
        buildEventRows()
        eventTitle.setOnClickListener {
            eventExpanded = !eventExpanded
            eventTitle.text = if (eventExpanded) "事件提醒 ▼" else "事件提醒 ▶"
            eventRows.visibility = if (eventExpanded) android.view.View.VISIBLE else android.view.View.GONE
        }

        list.addView(spacer(0, dp(20)))

        // 包裹提醒列表
        val pkgTitle = sectionTitle("包裹提醒 ▼")
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(pkgTitle, LinearLayout.LayoutParams(0, -2, 1f))
        })
        pkgRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(pkgRows)
        buildPkgRows()
        pkgTitle.setOnClickListener {
            pkgExpanded = !pkgExpanded
            pkgTitle.text = if (pkgExpanded) "包裹提醒 ▼" else "包裹提醒 ▶"
            pkgRows.visibility = if (pkgExpanded) android.view.View.VISIBLE else android.view.View.GONE
        }

        return ScrollView(this).apply { addView(list) }
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

    private fun packageRow(name: String, desc: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundRect(0xFFFFFFFF.toInt(), dp(8))
            elevation = dp(1).toFloat()

            addView(dot(0xFF2196F3.toInt()))
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
        }
    }

    // ═══════════════════════════════════════════
    // DB 列表构建
    // ═══════════════════════════════════════════

    private fun buildSmsRows() {
        smsRows.removeAllViews()
        val list = db.queryAllSms()
        if (list.isEmpty()) {
            smsRows.addView(TextView(this).apply {
                text = "暂无短信"
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
        val timeStr = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(sms.date))
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
                text = "$dateStr  $timeStr"
                textSize = 11f
                setTextColor(0xFF9E9E9E.toInt())
                setPadding(0, dp(4), 0, 0)
            })
            setOnClickListener {
                val fullTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(sms.date))
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("短信详情")
                    .setMessage("$fullTime\n\n${sms.body}")
                    .setPositiveButton("关闭", null)
                    .show()
            }
        }
    }

    private fun buildEventRows() {
        eventRows.removeAllViews()
        eventRows.addView(spacer(0, dp(8)))
        val eventFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        db.queryAllEvents().forEach { e ->
            val dateStr = if (e.date > 0) eventFmt.format(Date(e.date)) else "待定"
            val name = Regex("""【(.+?)】""").find(e.body)?.groupValues?.getOrNull(1) ?: "事件"
            val row = eventRow(name, dateStr, e.body, eventColor(e.date))
            row.setOnClickListener { showEventDetail(e) }
            val swiped = swipeWrap(row) { db.deleteEvent(e.id); buildEventRows() }
            eventRows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }

    private fun eventColor(dueDate: Long): String {
        if (dueDate == 0L) return "blue"
        val days = (dueDate - System.currentTimeMillis()) / 86_400_000
        return when { days <= 3 -> "red"; days <= 7 -> "orange"; else -> "blue" }
    }

    private fun showEventDetail(e: Event) {
        val dateStr = if (e.date > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(e.date)) else "待定"
        android.app.AlertDialog.Builder(this)
            .setTitle(dateStr)
            .setMessage(e.body)
            .setPositiveButton("编辑") { _, _ -> showEditEventDialog(e) }
            .setNegativeButton("关闭", null)
            .show()
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
            val desc = p.description.ifEmpty { p.address.ifEmpty { "暂无信息" } }
            val row = packageRow(p.company, desc)
            row.setOnClickListener {
                android.app.AlertDialog.Builder(this)
                    .setTitle(p.company)
                    .setMessage(p.description)
                    .setPositiveButton("关闭", null)
                    .show()
            }
            val swiped = swipeWrap(row) { db.deletePackage(p.id); buildPkgRows() }
            pkgRows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }
}
