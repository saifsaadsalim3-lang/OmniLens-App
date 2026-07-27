package com.omnilens.omniguard

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val rootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(60, 120, 60, 60)
                setBackgroundColor(Color.parseColor("#0F172A"))
            }

            val titleTv = TextView(this).apply {
                text = "🛡️ منظومة OmniLens Engine v2.0"
                textSize = 20f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 30)
            }

            val descTv = TextView(this).apply {
                text = "المحرك الرسمي الموحد لتوثيق الصور وحفظ الحقوق الرقمية.\nاختر أحد الخيارات للبدء:"
                textSize = 14f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 0, 0, 40)
            }

            val btnCameraBack = Button(this).apply {
                text = "📷 التقاط بالكاميرا الخلفية"
                setOnClickListener { openCamera(isFront = false) }
            }

            val btnCameraFront = Button(this).apply {
                text = "🤳 التقاط بالكاميرا الأمامية"
                setOnClickListener { openCamera(isFront = true) }
            }

            val btnGallery = Button(this).apply {
                text = "📁 اختيار صورة من المعرض وتوثيقها"
                setOnClickListener { openGallery() }
            }

            rootLayout.addView(titleTv)
            rootLayout.addView(descTv)
            rootLayout.addView(btnCameraBack)
            rootLayout.addView(btnCameraFront)
            rootLayout.addView(btnGallery)

            setContentView(rootLayout)

            if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                streamUri?.let { uri ->
                    currentImageUri = uri
                    showLicenseSelectionDialog()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في التشغيل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showLicenseSelectionDialog() {
        val licenseTypes = arrayOf(
            "🟢 ترخيص تجاري مدفوع",
            "🔵 إهداء خاص",
            "🔴 ملكية خاصة (يمنع النشر)",
            "⚪ ترخيص مجاني (استخدام عام)"
        )

        AlertDialog.Builder(this)
            .setTitle("اختر نوع التوثيق — OmniLens")
            .setItems(licenseTypes) { _, which ->
                currentImageUri?.let { uri ->
                    processAndDrawWatermark(uri, which)
                } ?: run {
                    Toast.makeText(this, "يرجى اختيار صورة أولاً", Toast.LENGTH_SHORT).show()
                    openGallery()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun processAndDrawWatermark(imageUri: Uri, licenseTypeIndex: Int) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Toast.makeText(this, "تعذر قراءة الصورة", Toast.LENGTH_SHORT).show()
                return
            }

            val bannerHeight = (originalBitmap.height * 0.12f).coerceAtLeast(140f).toInt()
            val newBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height + bannerHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(newBitmap)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            val (bgColor, headerText, detailText) = when (licenseTypeIndex) {
                0 -> Triple(
                    Color.parseColor("#15803D"),
                    "🟢 ترخيص تجاري مدفوع — Commercial License",
                    "حقوق التداول والاستخدام محددة بموجب ترخيص OmniLens الرقمي"
                )
                1 -> Triple(
                    Color.parseColor("#1D4ED8"),
                    "🔵 إهداء خاص — Private Gift",
                    "محتوى خاص مُهدى بموجب حقوق المالك الأصلي"
                )
                2 -> Triple(
                    Color.parseColor("#B91C1C"),
                    "🔴 ملكية خاصة — يمنع النشر أو التداول (PROPRIETARY)",
                    "محمي جنائياً | المالك: saifsaadsalim3@gmail.com | يحظر التداول بدون إذن"
                )
                else -> Triple(
                    Color.parseColor("#475569"),
                    "⚪ ترخيص مجاني — Free Public License",
                    "محتوى عام موثق بواسطة منصة OmniLens Engine"
                )
            }

            val paint = Paint().apply { isAntiAlias = true }
            paint.color = bgColor
            val bannerRect = RectF(
                0f,
                originalBitmap.height.toFloat(),
                originalBitmap.width.toFloat(),
                (originalBitmap.height + bannerHeight).toFloat()
            )
            canvas.drawRect(bannerRect, paint)

            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sha256Hash = generateSHA256("$timeStamp-$licenseTypeIndex-${originalBitmap.width}")

            val textPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                textSize = bannerHeight * 0.22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val startX = 30f
            var startY = originalBitmap.height + (bannerHeight * 0.32f)

            canvas.drawText(headerText, startX, startY, textPaint)

            textPaint.textSize = bannerHeight * 0.16f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            startY += bannerHeight * 0.26f
            canvas.drawText(detailText, startX, startY, textPaint)

            textPaint.textSize = bannerHeight * 0.13f
            textPaint.color = Color.parseColor("#CBD5E1")
            startY += bannerHeight * 0.22f
            canvas.drawText("SHA-256: ${sha256Hash.take(32)}... | Date: $timeStamp", startX, startY, textPaint)

            saveAndShareProcessedImage(newBitmap)

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ أثناء معالجة الصورة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateSHA256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun saveAndShareProcessedImage(bitmap: Bitmap) {
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "OmniLens_Secured_${System.currentTimeMillis()}.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "مشاركة الصورة الموثقة بشريط OmniLens"))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر مشاركة الصورة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openCamera(isFront: Boolean) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (isFront) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        } else {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 0)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الكاميرا: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(intent, 1001)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح المعرض: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                currentImageUri = uri
                showLicenseSelectionDialog()
            }
        }
    }
}
