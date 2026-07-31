package com.example.smsreader

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun roundRect(color: Int, radius: Int) =
    GradientDrawable().apply { setColor(color); cornerRadius = radius.toFloat() }

fun Context.divider(color: Int) = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(-1, 1)
    setBackgroundColor(color)
}

fun Context.dot(color: Int) = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
    background = GradientDrawable().apply { setColor(color); shape = GradientDrawable.OVAL }
}

fun Context.spacer(w: Int, h: Int) = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(w, h)
}

fun Context.sectionTitle(text: String) = TextView(this).apply {
    this.text = text
    textSize = 14f
    setTextColor(0xFF757575.toInt())
    setTypeface(null, Typeface.BOLD)
}

fun Context.detailHeader(title: String) = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    setPadding(dp(8), dp(12), dp(16), dp(12))
    setBackgroundColor(0xFFFFFFFF.toInt())
    elevation = dp(2).toFloat()

    addView(TextView(this@detailHeader).apply {
        text = "←"
        textSize = 20f
        setTextColor(0xFF1976D2.toInt())
        setPadding(dp(8), 0, dp(16), 0)
        setOnClickListener { (context as? android.app.Activity)?.finish() }
    })
    addView(TextView(this@detailHeader).apply {
        text = title
        textSize = 18f
        setTextColor(0xFF212121.toInt())
        setTypeface(null, Typeface.BOLD)
    })
}

fun Context.swipeWrap(row: View, onDelete: () -> Unit): FrameLayout {
    val trashW = dp(64)
    val frame = FrameLayout(this)
    var startX = 0f
    var startTx = 0f
    var startY = 0f
    var tracking = false

    val trash = TextView(this).apply {
        text = "🗑"
        textSize = 20f
        gravity = Gravity.CENTER
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFFE53935.toInt())
    }
    frame.addView(trash, FrameLayout.LayoutParams(trashW, -1, Gravity.END or Gravity.CENTER_VERTICAL))
    frame.addView(row, FrameLayout.LayoutParams(-1, -2))

    row.setOnTouchListener { _, e ->
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = e.rawX; startY = e.rawY; startTx = row.translationX; tracking = false
                false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - startX; val dy = e.rawY - startY
                if (!tracking && (kotlin.math.abs(dx) > dp(8) || kotlin.math.abs(dy) > dp(8))) tracking = true
                if (tracking && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                    frame.parent?.requestDisallowInterceptTouchEvent(true)
                    row.translationX = (startTx + dx).coerceIn(-trashW.toFloat(), 0f)
                    true
                } else false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                frame.parent?.requestDisallowInterceptTouchEvent(false)
                if (tracking) {
                    val snapOpen = row.translationX < -trashW / 3
                    row.animate().translationX(if (snapOpen) -trashW.toFloat() else 0f)
                    true
                } else if (startTx < -1) {
                    row.animate().translationX(0f)
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    trash.setOnClickListener {
        row.animate().translationX(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) { onDelete() }
        })
    }

    return frame
}
