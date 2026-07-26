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
     * دالة توليد صورة موثقة تحتوي على الشريط السفلي (Smart Certification Bar)
     */
    fun createCertifiedImage(
        originalBitmap: Bitmap,
        recipientIdentifier: String, // رقم الهاتف أو البريد الإلكتروني للمستلم
        licenseType: String = "ترخيص تجاري مدفوع (Paid)"
    ): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height

        // 1. حساب ارتفاع الشريط السفلي (12% من ارتفاع الصورة الأصلية)
        val bannerHeight = (height * 0.12f).toInt().coerceAtLeast(120)
        val totalHeight = height + bannerHeight

        // 2. إنشاء المساحة الكاملة للصورة الأصلية + الشريط السفلي
        val certifiedBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(certifiedBitmap)

        // 3. رسم الصورة الأصلية صافية ونقية في الأعلى 100%
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)

        // 4. رسم خلفية الشريط السفلي (لون داكن أنيق)
        val bannerPaint = Paint().apply {
            color = Color.parseColor("#1A1A1E")
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(0f, height.toFloat(), width.toFloat(), totalHeight.toFloat()), bannerPaint)

        // 5. رسم الخط الفاصل الذهبي أعلى الشريط
        val borderPaint = Paint().apply {
            color = Color.parseColor("#D4AF37")
            strokeWidth = (height * 0.005f).coerceAtLeast(4f)
        }
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), borderPaint)

        // 6. إعداد خطوط وألوان النصوص
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = bannerHeight * 0.22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            textSize = bannerHeight * 0.16f
        }

        val hashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            textSize = bannerHeight * 0.13f
            typeface = Typeface.MONOSPACE
        }

        // 7. حساب بصمة SHA-256 للصورة الأصلية
        val imageHash = generateSHA256(originalBitmap)

        // 8. كتابة بيانات التوثيق داخل الشريط السفلي
        val paddingX = width * 0.04f
        var currentY = height + (bannerHeight * 0.30f)

        canvas.drawText("🔒 منصة OmniLens — توثيق الملكية الرقمية", paddingX, currentY, titlePaint)
        currentY += bannerHeight * 0.25f
        canvas.drawText("👤 المرخص له: $recipientIdentifier | 📌 $licenseType", paddingX, currentY, detailPaint)
        currentY += bannerHeight * 0.22f
        val shortHash = if (imageHash.length > 32) "${imageHash.substring(0, 32)}..." else imageHash
        canvas.drawText("🔑 SHA-256: $shortHash", paddingX, currentY, hashPaint)

        return certifiedBitmap
    }

    /**
     * دالة توليد بصمة SHA-256 للبكسلات
     */
    private fun generateSHA256(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(stream.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
