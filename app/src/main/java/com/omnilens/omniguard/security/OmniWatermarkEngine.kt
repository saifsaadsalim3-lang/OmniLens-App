package com.omnilens.omniguard.security

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object OmniWatermarkEngine {

    /**
     * دالة توليد الصورة الموثقة بشريط OmniLens الذكي
     * تدعم الحالات الثلاث: (مدفوع / إهداء / ملكية خاصة)
     */
    fun createCertifiedImage(
        originalBitmap: Bitmap,
        recipientPhone: String,
        recipientEmail: String,
        licenseType: String = "ترخيص تجاري مدفوع",
        isPrivateMode: Boolean = false
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height

        // 1. حساب ارتفاع الشريط السفلي (14% من ارتفاع الصورة)
        val bannerHeight = (height * 0.14f).toInt().coerceAtLeast(140)
        val totalHeight = height + bannerHeight

        // 2. إنشاء المساحة الكاملة للصورة + الشريط السفلي
        val certifiedBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(certifiedBitmap)

        // 3. رسم الصورة الأصلية
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // 4. رسم خلفية الشريط السفلي (داكنة أنيقة)
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#1A1A1E")
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(0f, height.toFloat(), width.toFloat(), totalHeight.toFloat()), bannerPaint)

        // 5. رسم الخط الفاصل (ذهبي للمدفوع والمهدى / أحمر ياقوتي للخاصة)
        val borderPaint = Paint().apply {
            color = if (isPrivateMode) Color.parseColor("#C0392B") else Color.parseColor("#D4AF37")
            strokeWidth = (height * 0.005f).coerceAtLeast(4f)
        }
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), borderPaint)

        // 6. إعداد الخطوط
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = bannerHeight * 0.20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            textSize = bannerHeight * 0.15f
        }

        val hashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isPrivateMode) Color.parseColor("#E74C3C") else Color.parseColor("#D4AF37")
            textSize = bannerHeight * 0.12f
            typeface = Typeface.MONOSPACE
        }

        // 7. حساب بصمة SHA-256
        val imageHash = generateSHA256(originalBitmap)

        // 8. كتابة النصوص داخل الشريط السفلي
        val paddingX = width * 0.04f
        var currentY = height + (bannerHeight * 0.28f)

        // السطر الأول: العبارة الموحدة الجديدة
        canvas.drawText("🔒 جميع الحقوق محفوظة بواسطة OmniLens", paddingX, currentY, titlePaint)

        // السطر الثاني: البيانات المزدوجة ونوع الترخيص
        currentY += bannerHeight * 0.24f
        val ownerLabel = if (isPrivateMode) "المالك الأصلي" else "المرخص له"
        val recipientDetails = "👤 $ownerLabel: $recipientPhone | ✉️ $recipientEmail | 📌 $licenseType"
        canvas.drawText(recipientDetails, paddingX, currentY, detailPaint)

        // السطر الثالث: بصمة التشفير الجنائية
        currentY += bannerHeight * 0.22f
        val shortHash = if (imageHash.length > 32) "${imageHash.substring(0, 32)}..." else imageHash
        canvas.drawText("🔑 SHA-256: $shortHash", paddingX, currentY, hashPaint)

        return certifiedBitmap
    }

    private fun generateSHA256(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(stream.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
