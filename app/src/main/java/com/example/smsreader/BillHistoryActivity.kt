package com.example.smsreader

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BillHistoryActivity : AppCompatActivity() {

    private lateinit var db: SmsDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(detailHeader("历史账单"))
            addView(content(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun content(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        // 月账单
        list.addView(sectionTitle("月账单"))
        list.addView(spacer(0, dp(8)))
        val monthly = db.queryMonthlyBills()
        if (monthly.isEmpty()) {
            list.addView(emptyText("暂无月账单"))
        } else {
            monthly.forEach { m ->
                list.addView(monthCard(m), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }

        list.addView(spacer(0, dp(20)))

        // 年账单
        list.addView(sectionTitle("年账单"))
        list.addView(spacer(0, dp(8)))
        val annual = db.queryAnnualBills()
        if (annual.isEmpty()) {
            list.addView(emptyText("暂无年账单"))
        } else {
            annual.forEach { a ->
                list.addView(yearCard(a), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            }
        }

        return ScrollView(this).apply { addView(list) }
    }

    private fun monthCard(m: MonthlyBill): LinearLayout {
        val net = m.income - m.expense
        val netColor = if (net >= 0) 0xFF4CAF50.toInt() else 0xFFE53935.toInt()
        val netSign = if (net >= 0) "+" else ""

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundRect(0xFFFFFFFF.toInt(), dp(12))
            elevation = dp(1).toFloat()

            addView(TextView(this@BillHistoryActivity).apply {
                text = "${m.year}年${m.month}月"
                textSize = 16f; setTextColor(0xFF212121.toInt()); setTypeface(null, Typeface.BOLD)
            })
            addView(LinearLayout(this@BillHistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
                addView(statItem("收入", m.income, 0xFF4CAF50.toInt()))
                addView(statItem("支出", m.expense, 0xFFE53935.toInt()))
                addView(statItem("净额", net, netColor, netSign))
            })
        }
    }

    private fun yearCard(a: AnnualBill): LinearLayout {
        val net = a.income - a.expense
        val netColor = if (net >= 0) 0xFF4CAF50.toInt() else 0xFFE53935.toInt()
        val netSign = if (net >= 0) "+" else ""

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundRect(0xFF673AB7.toInt(), dp(12))
            elevation = dp(2).toFloat()

            addView(TextView(this@BillHistoryActivity).apply {
                text = "${a.year}年"
                textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(null, Typeface.BOLD)
            })
            addView(LinearLayout(this@BillHistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
                addView(statItemLight("收入", a.income))
                addView(statItemLight("支出", a.expense))
                addView(statItemLight("净额", net, netSign))
            })
        }
    }

    private fun statItem(label: String, amount: Double, color: Int, sign: String = ""): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@BillHistoryActivity).apply {
                text = label; textSize = 11f; setTextColor(0xFF9E9E9E.toInt())
            })
            addView(TextView(this@BillHistoryActivity).apply {
                text = "$sign¥${String.format("%,.2f", amount)}"
                textSize = 15f; setTextColor(color); setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(2), 0, 0)
            })
        }.also { it.layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
    }

    private fun statItemLight(label: String, amount: Double, sign: String = ""): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(this@BillHistoryActivity).apply {
                text = label; textSize = 11f; setTextColor(0xBBFFFFFF.toInt())
            })
            addView(TextView(this@BillHistoryActivity).apply {
                text = "$sign¥${String.format("%,.2f", amount)}"
                textSize = 15f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(null, Typeface.BOLD)
                setPadding(0, dp(2), 0, 0)
            })
        }.also { it.layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
    }

    private fun emptyText(msg: String) = TextView(this).apply {
        text = msg; textSize = 13f; setTextColor(0xFF9E9E9E.toInt())
        gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(16))
    }
}
