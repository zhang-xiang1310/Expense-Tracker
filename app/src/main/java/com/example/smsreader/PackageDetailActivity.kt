package com.example.smsreader

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PackageDetailActivity : AppCompatActivity() {

    private lateinit var db: SmsDbHelper
    private lateinit var activeRows: LinearLayout
    private lateinit var doneTitle: TextView
    private lateinit var doneRows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(detailHeader("包裹提醒"))
            addView(createContent(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun createContent(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        list.addView(sectionTitle("进行中"))
        activeRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(activeRows)

        doneTitle = sectionTitle("已签收")
        list.addView(doneTitle)
        doneRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(doneRows)

        buildRows()

        return ScrollView(this).apply { addView(list) }
    }

    private fun buildRows() {
        val pkgs = db.queryAllPackages()
        val active = pkgs.filter { !it.status.contains("签收") }
        val done = pkgs.filter { it.status.contains("签收") }

        activeRows.removeAllViews()
        activeRows.addView(spacer(0, dp(8)))
        active.forEach { p ->
            val status = p.status.ifEmpty { "处理中" }
            val desc = p.description.ifEmpty { p.address.ifEmpty { "暂无信息" } }
            val c = when {
                status.contains("派送") -> "green"
                status.contains("运输") -> "blue"
                else -> "orange"
            }
            val row = cardRow(p.company, status, desc, c)
            val swiped = swipeWrap(row) { db.deletePackage(p.id); buildRows() }
            activeRows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        doneRows.removeAllViews()
        doneRows.addView(spacer(0, dp(8)))
        done.forEach { p ->
            val desc = p.description.ifEmpty { p.address.ifEmpty { "暂无信息" } }
            val row = cardRow(p.company, p.status.ifEmpty { "已签收" }, desc, "gray")
            val swiped = swipeWrap(row) { db.deletePackage(p.id); buildRows() }
            doneRows.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun cardRow(name: String, status: String, desc: String, color: String): LinearLayout {
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
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundRect(0xFFFFFFFF.toInt(), dp(12))
            elevation = dp(1).toFloat()

            addView(dot(dotColor))
            addView(LinearLayout(this@PackageDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(TextView(this@PackageDetailActivity).apply {
                    text = name
                    textSize = 15f
                    setTextColor(0xFF212121.toInt())
                })
                addView(TextView(this@PackageDetailActivity).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(0xFF9E9E9E.toInt())
                    setPadding(0, dp(2), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@PackageDetailActivity).apply {
                text = status
                textSize = 13f
                setTextColor(statusColor)
            })
        }
    }
}
