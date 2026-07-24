package com.omnilens.omniguard

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40, 40, 40, 40)
        }

        // عرض صورة اللوجو
        val logoImage = ImageView(this).apply {
            val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (imageResId != 0) {
                setImageResource(imageResId)
            }
            layoutParams = LinearLayout.LayoutParams(400, 400).apply {
                gravity = Gravity.CENTER
            }
        }

        val titleText = TextView(this).apply {
            text = "OMNILENS"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }

        val statusText = TextView(this).apply {
            text = "نظام التشفير والحماية مفعل ✅"
            textSize = 16f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }

        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(statusText)

        setContentView(rootLayout)
    }
}
