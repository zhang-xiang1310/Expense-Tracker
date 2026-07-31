package com.example.smsreader

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
            addView(detailHeader("银行卡管理"))
            addView(createContent(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
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
        db.queryAllBankCards().forEach { card -> cardContainer.addView(cardItem(card)) }
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
            (layoutParams as? LinearLayout.LayoutParams)?.bottomMargin = dp(8)

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
