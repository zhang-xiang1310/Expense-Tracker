package com.example.smsreader

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class EventDetailActivity : AppCompatActivity() {

    private lateinit var db: SmsDbHelper
    private lateinit var rows: LinearLayout
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(detailHeader("事件提醒"))
            addView(createContent(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun createContent(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(rows)
        buildRows()

        return ScrollView(this).apply { addView(list) }
    }

    private fun buildRows() {
        rows.removeAllViews()
        db.queryAllEvents().forEach { e ->
            val row = eventCard(e)
            val swiped = swipeWrap(row) { db.deleteEvent(e.id); buildRows() }
            rows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun eventCard(e: Event): LinearLayout {
        val dateStr = if (e.date > 0) fmt.format(Date(e.date)) else "待定"
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundRect(0xFFFFFFFF.toInt(), dp(12))
            elevation = dp(1).toFloat()

            addView(dot(0xFF2196F3.toInt()))
            addView(TextView(this@EventDetailActivity).apply {
                text = "$dateStr  $e.body"
                textSize = 15f
                setTextColor(0xFF212121.toInt())
                setPadding(dp(12), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, -2, 1f))

            setOnClickListener { showEditEventDialog(e) }
        }
    }

    private fun showEditEventDialog(e: Event) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), 0)
        }
        val dateEdit = EditText(this).apply {
            hint = "日期（yyyy-MM-dd）"
            setText(if (e.date > 0) fmt.format(Date(e.date)) else "")
        }
        val bodyEdit = EditText(this).apply {
            hint = "事件内容"
            setText(e.body)
        }
        layout.addView(dateEdit)
        layout.addView(bodyEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        AlertDialog.Builder(this)
            .setTitle("编辑事件")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newDate = parseDate(dateEdit.text.toString().trim())
                val newBody = bodyEdit.text.toString().trim()
                if (newBody.isNotEmpty()) {
                    db.updateEvent(e.id, newDate, newBody)
                    buildRows()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun parseDate(s: String): Long {
        if (s.isEmpty()) return 0L
        return try { fmt.parse(s)?.time ?: 0L } catch (_: Exception) { 0L }
    }
}
