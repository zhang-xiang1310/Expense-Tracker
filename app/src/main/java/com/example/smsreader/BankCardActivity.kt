package com.example.smsreader

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BankCardActivity : AppCompatActivity() {

    private lateinit var db: SmsDbHelper
    private lateinit var cardContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(addCardHeader())
            addView(createContent(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun addCardHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(16), dp(12))
            setBackgroundColor(0xFFFFFFFF.toInt())
            elevation = dp(2).toFloat()

            addView(TextView(this@BankCardActivity).apply {
                text = "←"
                textSize = 20f
                setTextColor(0xFF1976D2.toInt())
                setPadding(dp(8), 0, dp(16), 0)
                setOnClickListener { finish() }
            })
            addView(TextView(this@BankCardActivity).apply {
                text = "银行卡管理"
                textSize = 18f
                setTextColor(0xFF212121.toInt())
                setTypeface(null, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, -2, 1f))

            addView(TextView(this@BankCardActivity).apply {
                text = "＋ 添加"
                textSize = 14f
                setTextColor(0xFF1976D2.toInt())
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener { showAddCardDialog() }
            })
        }
    }

    private fun showAddCardDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), 0)
        }
        val bankEdit = EditText(this).apply { hint = "银行名称（如：招商银行）" }
        val numberEdit = EditText(this).apply { hint = "卡号后四位" }
        val balanceEdit = EditText(this).apply { hint = "当前余额（如：12345.67）" }
        layout.addView(bankEdit)
        layout.addView(numberEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        layout.addView(balanceEdit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        android.app.AlertDialog.Builder(this)
            .setTitle("添加银行卡")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val bank = bankEdit.text.toString().trim()
                val number = numberEdit.text.toString().trim()
                val balance = balanceEdit.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (bank.isNotEmpty()) {
                    db.insertBankCard(bank, number, balance)
                    refreshCards()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createContent(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        cardContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(cardContainer)

        refreshCards()
        return ScrollView(this).apply { addView(list) }
    }

    private fun refreshCards() {
        cardContainer.removeAllViews()
        val cards = db.queryAllBankCards()
        if (cards.isEmpty()) {
            cardContainer.addView(TextView(this).apply {
                text = "暂无银行卡"
                textSize = 14f
                setTextColor(0xFF9E9E9E.toInt())
                gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, 0)
            })
        }
        cards.forEach { card ->
            val row = cardItem(card)
            val swiped = swipeWrap(row) { db.deleteBankCard(card.id); refreshCards() }
            cardContainer.addView(swiped, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun cardItem(card: BankCard): LinearLayout {
        val masked = if (card.number.length <= 4) card.number
        else "*".repeat(card.number.length - 4) + card.number.takeLast(4)

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundRect(0xFFFFFFFF.toInt(), dp(12))
            elevation = dp(1).toFloat()

            addView(LinearLayout(this@BankCardActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@BankCardActivity).apply {
                    text = card.bank
                    textSize = 16f
                    setTextColor(0xFF212121.toInt())
                    setTypeface(null, Typeface.BOLD)
                })
                addView(TextView(this@BankCardActivity).apply {
                    text = masked
                    textSize = 13f
                    setTextColor(0xFF757575.toInt())
                    setPadding(0, dp(4), 0, 0)
                })
            }, LinearLayout.LayoutParams(0, -2, 1f))

            addView(TextView(this@BankCardActivity).apply {
                text = String.format("¥ %,.2f", card.balance)
                textSize = 16f
                setTextColor(0xFF212121.toInt())
                setTypeface(null, Typeface.BOLD)
            })
        }
    }
}
