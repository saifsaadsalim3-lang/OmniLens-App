package com.omnilens.omniguard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

// 🌟 محرك مستقل تماماً لرسم الشعار البصري السيادي
object LogoEngine {

    fun createSovereignLogo(): Bitmap {
        val bitmap = Bitmap.createBitmap(140, 140, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // إطار الدرع الأزرق المضيء
        paint.color = Color.parseColor("#38BDF8")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        canvas.drawCircle(70f, 70f, 58f, paint)

        // جسم القفل والعدسة
        paint.color = Color.parseColor("#38BDF8")
        paint.style = Paint.Style.FILL
        canvas.drawRect(50f, 65f, 90f, 100f, paint)
        canvas.drawCircle(70f, 52f, 15f, paint)

        return bitmap
    }
}
