package com.example.smsreader

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CategoryManageActivity : AppCompatActivity() {

    private lateinit var db: SmsDbHelper
    private lateinit var kwContainer: LinearLayout
    private var curCategory = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SmsDbHelper(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(detailHeader("词库管理"))
            addView(content(), LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    private fun content(): ScrollView {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        list.addView(sectionTitle("分类"))
        list.addView(spacer(0, dp(8)))

        val kwMap = db.queryCategoryKeywords()
        kwMap.keys.forEach { cat ->
            list.addView(categoryBtn(cat, kwMap[cat]?.size ?: 0),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        list.addView(spacer(0, dp(16)))
        list.addView(sectionTitle("匹配词"))
        list.addView(spacer(0, dp(8)))
        kwContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(kwContainer)

        return ScrollView(this).apply { addView(list) }
    }

    private fun categoryBtn(cat: String, count: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundRect(0xFFFFFFFF.toInt(), dp(8))
            elevation = dp(1).toFloat()

            addView(TextView(this@CategoryManageActivity).apply {
                text = cat; textSize = 15f; setTextColor(0xFF212121.toInt())
                setTypeface(null, Typeface.BOLD)
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@CategoryManageActivity).apply {
                text = "$count 词"; textSize = 12f; setTextColor(0xFF9E9E9E.toInt())
            })
            setOnClickListener { showCategoryKws(cat) }
        }
    }

    private fun showCategoryKws(cat: String) {
        curCategory = cat
        kwContainer.removeAllViews()
        kwContainer.addView(TextView(this).apply {
            text = "＋ 添加匹配词"
            textSize = 13f; setTextColor(0xFF1976D2.toInt())
            setPadding(0, dp(4), 0, dp(8))
            setOnClickListener { showAddDialog() }
        })

        val kws = db.queryCategoryKwList(cat)
        kws.forEach { (id, kw) ->
            kwContainer.addView(kwRow(id, kw),
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        }
    }

    private fun kwRow(id: Long, kw: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(8), dp(8))
            background = roundRect(0xFFF5F5F5.toInt(), dp(6))

            addView(TextView(this@CategoryManageActivity).apply {
                text = kw; textSize = 14f; setTextColor(0xFF424242.toInt())
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(this@CategoryManageActivity).apply {
                text = "✎"; textSize = 16f; setTextColor(0xFF1976D2.toInt())
                setPadding(dp(12), dp(4), dp(8), dp(4))
                setOnClickListener { showEditDialog(id, kw) }
            })
            addView(TextView(this@CategoryManageActivity).apply {
                text = "✕"; textSize = 16f; setTextColor(0xFFE53935.toInt())
                setPadding(dp(4), dp(4), dp(4), dp(4))
                setOnClickListener {
                    db.deleteCategoryKw(id)
                    showCategoryKws(curCategory)
                }
            })
        }
    }

    private fun showAddDialog() {
        val edit = EditText(this).apply { hint = "输入匹配词（如：餐饮消费）" }
        AlertDialog.Builder(this)
            .setTitle("添加匹配词 → $curCategory")
            .setView(edit, dp(24), dp(16), dp(24), 0)
            .setPositiveButton("确定") { _, _ ->
                val kw = edit.text.toString().trim()
                if (kw.isNotEmpty()) {
                    db.insertCategoryKw(curCategory, kw)
                    showCategoryKws(curCategory)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditDialog(id: Long, oldKw: String) {
        val edit = EditText(this).apply { setText(oldKw) }
        AlertDialog.Builder(this)
            .setTitle("编辑匹配词")
            .setView(edit, dp(24), dp(16), dp(24), 0)
            .setPositiveButton("确定") { _, _ ->
                val kw = edit.text.toString().trim()
                if (kw.isNotEmpty()) {
                    db.updateCategoryKw(id, kw)
                    showCategoryKws(curCategory)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
