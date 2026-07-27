package com.omnilens.omniguard.security

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object OmniWatermarkEngine {

    // 🌐 رابط خادم التوثيق السحابي المباشر على Render
    private const val BASE_VERIFY_URL = "https://omnilens-verify.onrender.com/v/"

    /**
     * معالجة الصورة وإضافة شريط التوثيق والرابط الحي مع بصمة SHA-256
     */
    fun applyOmniLensWatermark(originalBitmap: Bitmap): Bitmap {
        // 1. حساب بصمة SHA-256 للصورة واقتطاع أول 10 أحرف لرمز البصمة المختصر
        val fullHash = calculateSHA256(originalBitmap)
        val shortHash = fullHash.take(10)
        
        // 2. تكوين رابط التوثيق المباشر
        val verifyUrl = "$BASE_VERIFY_URL$shortHash"

        // 3. إنشاء نسخة قابلة للتعديل من الصورة
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val width = mutableBitmap.width
        val height = mutableBitmap.height

        // 4. تحديد ارتفاع شريط التوثيق في أسفل الصورة
        val bannerHeight = (height * 0.08f).coerceAtLeast(140f)

        // 5. رسم خلفية الشريط (أسود داكن راقي)
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#E60D1117")
            style = Paint.Style.FILL
        }
        val bannerRect = RectF(0f, height - bannerHeight, width.toFloat(), height.toFloat())
        canvas.drawRect(bannerRect, bannerPaint)

        // 6. رسم خط أفق أخضر علوي للشريط (علامة الأمان)
        val linePaint = Paint().apply {
            color = Color.parseColor("#238636")
            strokeWidth = (bannerHeight * 0.04f).coerceAtLeast(5f)
        }
        canvas.drawLine(0f, height - bannerHeight, width.toFloat(), height - bannerHeight, linePaint)

        // 7. إعداد الخطوط والنصوص
        val titleFontSize = (bannerHeight * 0.28f).coerceAtLeast(26f)
        val bodyFontSize = (bannerHeight * 0.22f).coerceAtLeast(20f)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#2EA043")
            textSize = titleFontSize
            isFakeBoldText = true
            isAntiAlias = true
        }

        val urlPaint = Paint().apply {
            color = Color.WHITE
            textSize = bodyFontSize
            isAntiAlias = true
        }

        // 8. رسم النصوص على الشريط
        val paddingX = width * 0.04f
        val startY = height - bannerHeight + (bannerHeight * 0.38f)

        canvas.drawText("✔ صورة موثقة | OmniLens Engine 2026", paddingX, startY, titlePaint)
        canvas.drawText("Verify: $verifyUrl", paddingX, startY + (bannerHeight * 0.35f), urlPaint)

        return mutableBitmap
    }

    /**
     * خوارزمية حساب بصمة SHA-256 لمصفوفة بيانات الصورة
     */
    private fun calculateSHA256(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val byteArray = stream.toByteArray()

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(byteArray)

        val hexString = StringBuilder()
        for (b in hashBytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }
}
