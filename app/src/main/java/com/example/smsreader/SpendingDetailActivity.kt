package com.example.smsreader

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class SpendingDetailActivity : AppCompatActivity() {

    private lateinit var db: SmsDbHelper
    private lateinit var todayRows: LinearLayout
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 86400000L
        db.deleteBillsBefore(sevenDaysAgo)

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(detailHeader("收支统计"))
            addView(createContent(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun createContent(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        list.addView(sectionTitle("今天"))
        list.addView(spacer(0, dp(8)))
        list.addView(todaySummaryCard())
        list.addView(spacer(0, dp(8)))
        todayRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(todayRows)
        buildTodayBills()

        list.addView(spacer(0, dp(20)))

        list.addView(sectionTitle("本月统计"))
        list.addView(spacer(0, dp(8)))
        list.addView(monthSummaryCard())

        list.addView(spacer(0, dp(20)))

        list.addView(sectionTitle("本年统计"))
        list.addView(spacer(0, dp(8)))
        list.addView(yearSummaryCard())

        return ScrollView(this).apply { addView(list) }
    }

    // ═══════════════════════════════════
    // 今天详细账单
    // ═══════════════════════════════════

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        return Pair(start, System.currentTimeMillis())
    }

    private fun todaySummaryCard(): LinearLayout {
        val (start, end) = todayRange()
        val (income, expense) = db.sumBillsBetween(start, end)
        return statCard("今日", income, expense, 0xFF2196F3.toInt(), false)
    }

    private fun buildTodayBills() {
        todayRows.removeAllViews()
        val (start, end) = todayRange()
        val bills = db.queryBillsBetween(start, end)
        if (bills.isEmpty()) {
            todayRows.addView(TextView(this).apply {
                text = "暂无账单"
                textSize = 13f; setTextColor(0xFF9E9E9E.toInt())
                gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(16))
            })
            return
        }
        bills.forEach { b ->
            todayRows.addView(billItem(b), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
        }
    }

    private fun billItem(b: Bill): LinearLayout {
        val isIncome = b.category == "收入"
        val sign = if (isIncome) "+" else "-"
        val color = if (isIncome) 0xFF4CAF50.toInt() else 0xFFE53935.toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundRect(0xFFFFFFFF.toInt(), dp(10))
            elevation = dp(1).toFloat()

            addView(LinearLayout(this@SpendingDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@SpendingDetailActivity).apply {
                    text = b.bankName.ifEmpty { "未知" }.take(30)
                    textSize = 14f; setTextColor(0xFF212121.toInt())
                })
                addView(TextView(this@SpendingDetailActivity).apply {
                    text = "${timeFmt.format(Date(b.date))}  ${b.type}".trim()
                    textSize = 11f; setTextColor(0xFF9E9E9E.toInt())
                    setPadding(0, dp(3), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@SpendingDetailActivity).apply {
                text = "$sign¥${String.format("%.2f", b.amount)}"
                textSize = 15f; setTextColor(color); setTypeface(null, Typeface.BOLD)
            })

            setOnClickListener { showEditBillDialog(b) }
        }
    }

    private fun showEditBillDialog(b: Bill) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), 0)
        }

        val amtEdit = EditText(this).apply {
            hint = "金额"
            setText(String.format("%.2f", b.amount))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val bankEdit = EditText(this).apply { hint = "银行"; setText(b.bankName) }
        val catEdit = EditText(this).apply { hint = "类别（如：伙食、转账）"; setText(b.category) }
        val typeEdit = EditText(this).apply { hint = "类型"; setText(b.type) }

        layout.addView(amtEdit)
        layout.addView(bankEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        layout.addView(catEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        layout.addView(typeEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        AlertDialog.Builder(this)
            .setTitle("编辑账单")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val amt = amtEdit.text.toString().trim().toDoubleOrNull() ?: return@setPositiveButton
                val bank = bankEdit.text.toString().trim()
                val cat = catEdit.text.toString().trim()
                val typ = typeEdit.text.toString().trim()
                if (bank.isNotEmpty() && cat.isNotEmpty()) {
                    db.updateBill(b.id, amt, bank, cat, b.date, typ)
                    buildTodayBills()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ═══════════════════════════════════
    // 本月统计
    // ═══════════════════════════════════

    private fun monthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }

    private fun monthSummaryCard(): LinearLayout {
        val (start, end) = monthRange()
        val (income, expense) = db.sumBillsBetween(start, end)
        return statCard("本月", income, expense, 0xFF009688.toInt())
    }

    // ═══════════════════════════════════
    // 本年统计
    // ═══════════════════════════════════

    private fun yearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return Pair(cal.timeInMillis, System.currentTimeMillis())
    }

    private fun yearSummaryCard(): LinearLayout {
        val (start, end) = yearRange()
        val (income, expense) = db.sumBillsBetween(start, end)
        return statCard("本年", income, expense, 0xFF673AB7.toInt())
    }

    // ═══════════════════════════════════
    // 通用统计卡片
    // ═══════════════════════════════════

    private fun statCard(label: String, income: Double, expense: Double, color: Int, showNet: Boolean = true): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundRect(color, dp(14))
            elevation = dp(2).toFloat()

            addView(TextView(this@SpendingDetailActivity).apply {
                text = label
                textSize = 13f; setTextColor(0xCCFFFFFF.toInt())
            })
            addView(LinearLayout(this@SpendingDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
                addView(LinearLayout(this@SpendingDetailActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(this@SpendingDetailActivity).apply {
                        text = "收入"; textSize = 11f; setTextColor(0x99FFFFFF.toInt())
                    })
                    addView(TextView(this@SpendingDetailActivity).apply {
                        text = "¥${String.format("%,.2f", income)}"
                        textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(null, Typeface.BOLD)
                    })
                }, LinearLayout.LayoutParams(0, -2, 1f))
                addView(LinearLayout(this@SpendingDetailActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(this@SpendingDetailActivity).apply {
                        text = "支出"; textSize = 11f; setTextColor(0x99FFFFFF.toInt())
                    })
                    addView(TextView(this@SpendingDetailActivity).apply {
                        text = "¥${String.format("%,.2f", expense)}"
                        textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(null, Typeface.BOLD)
                    })
                }, LinearLayout.LayoutParams(0, -2, 1f))
                if (showNet) {
                    addView(LinearLayout(this@SpendingDetailActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(TextView(this@SpendingDetailActivity).apply {
                            text = "净额"; textSize = 11f; setTextColor(0x99FFFFFF.toInt())
                        })
                        addView(TextView(this@SpendingDetailActivity).apply {
                            val net = income - expense
                            text = (if (net >= 0) "+" else "") + "¥${String.format("%,.2f", net)}"
                            textSize = 18f; setTextColor(0xFFFFFFFF.toInt()); setTypeface(null, Typeface.BOLD)
                        })
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                }
            })
        }
    }
}
