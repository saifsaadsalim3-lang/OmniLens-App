package com.omnilens.omniguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var currentImageUri: Uri? = null
    private var cameraTempUri: Uri? = null

    private val REQUEST_CAMERA_PERMISSION = 2001
    private val REQUEST_CAMERA_CAPTURE = 1002
    private val REQUEST_GALLERY_PICK = 1001

    // قائمة القطاعات التخصصية المعتمدة
    private val sectors = arrayOf(
        "⚽ القطاع الرياضي والفعاليات (Sports & Events)",
        "📰 الصحافة والإعلام (Journalism & Press)",
        "🏥 القطاع الطبي والصحي (Medical & Health)",
        "🎬 الإنتاج السينمائي والمرئي (Cinematic Master)",
        "🎨 الفن الرقمي والتصميم (Fine Art & NFT)",
        "📐 الهندسة والمخططات (Engineering & Architecture)",
        "🎓 القطاع الأكاديمي والبحثي (Academic & Research)"
    )

    // مستويات التراخيص الأربعة المتاحة لكل قطاع
    private val licenseTiers = arrayOf(
        "🟢 ترخيص تجاري مدفوع (Commercial License)",
        "🔵 إهداء خاص (Private Gift)",
        "🔴 ملكية خاصة — يمنع النشر (PROPRIETARY)",
        "⚪ ترخيص مجاني (Free Public License)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val scrollView = ScrollView(this).apply {
                setBackgroundColor(Color.parseColor("#0F172A"))
            }

            val rootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 80, 50, 80)
            }

            val titleTv = TextView(this).apply {
                text = "🛡️ منظومة OmniLens Engine v2.0"
                textSize = 22f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 20)
            }

            val descTv = TextView(this).apply {
                text = "منظومة الحماية والتشفير الرقمي للأصول الإعلامية والتخصصية:"
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 0, 0, 40)
            }

            // قسم الالتقاط والجلب
            val sectionSourceTitle = TextView(this).apply {
                text = "📸 وسائل التقاط وجلب الوسائط"
                textSize = 15f
                setTextColor(Color.parseColor("#38BDF8"))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 10, 0, 20)
            }

            val btnCameraBack = Button(this).apply {
                text = "📷 التقاط بالكاميرا الخلفية"
                setOnClickListener { checkPermissionAndOpenCamera(isFront = false) }
            }

            val btnCameraFront = Button(this).apply {
                text = "🤳 التقاط بالكاميرا الأمامية"
                setOnClickListener { checkPermissionAndOpenCamera(isFront = true) }
            }

            val btnGallery = Button(this).apply {
                text = "📁 اختيار صورة من المعرض وتوثيقها"
                setOnClickListener { openGallery() }
            }

            // قسم الأقسام التخصصية المباشرة
            val sectionCategoryTitle = TextView(this).apply {
                text = "🏷️ القطاعات التخصصية المعتمدة"
                textSize = 15f
                setTextColor(Color.parseColor("#38BDF8"))
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 40, 0, 20)
            }

            rootLayout.addView(titleTv)
            rootLayout.addView(descTv)
            rootLayout.addView(sectionSourceTitle)
            rootLayout.addView(btnCameraBack)
            rootLayout.addView(btnCameraFront)
            rootLayout.addView(btnGallery)
            rootLayout.addView(sectionCategoryTitle)

            // إضافة أزرار القطاعات ديناميكياً
            sectors.forEachIndexed { index, sectorName ->
                val btnSector = Button(this).apply {
                    text = sectorName
                    setOnClickListener { startDocumentationFlow(index) }
                }
                rootLayout.addView(btnSector)
            }

            scrollView.addView(rootLayout)
            setContentView(scrollView)

            // استقبال مشاركات الصور من الواتساب والتطبيق الخارجي
            if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                streamUri?.let { uri ->
                    currentImageUri = uri
                    promptSectorSelection()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في التشغيل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startDocumentationFlow(sectorIndex: Int) {
        if (currentImageUri != null) {
            promptLicenseTierSelection(sectorIndex)
        } else {
            Toast.makeText(this, "يرجى اختيار أو التقاط صورة أولاً", Toast.LENGTH_SHORT).show()
            openGallery()
        }
    }

    private fun promptSectorSelection() {
        AlertDialog.Builder(this)
            .setTitle("اختر القطاع التخصصي للصورة")
            .setItems(sectors) { _, sectorIndex ->
                promptLicenseTierSelection(sectorIndex)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun promptLicenseTierSelection(sectorIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("اختر نوع الترخيص — ${sectors[sectorIndex].split("(")[0]}")
            .setItems(licenseTiers) { _, tierIndex ->
                currentImageUri?.let { uri ->
                    processAndDrawWatermark(uri, sectorIndex, tierIndex)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun processAndDrawWatermark(imageUri: Uri, sectorIndex: Int, tierIndex: Int) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Toast.makeText(this, "تعذر قراءة ملف الصورة", Toast.LENGTH_SHORT).show()
                return
            }

            val bannerHeight = (originalBitmap.height * 0.14f).coerceAtLeast(180f).toInt()
            val newBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height + bannerHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(newBitmap)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)

            // 1. تحديد لون خلفية الشريط بحسب مستوى الترخيص
            val bannerColor = when (tierIndex) {
                0 -> Color.parseColor("#15803D") // أخضر تجاري
                1 -> Color.parseColor("#1D4ED8") // أزرق إهداء
                2 -> Color.parseColor("#B91C1C") // أحمر ملكية خاصة
                else -> Color.parseColor("#475569") // رمادي مجاني
            }

            val paint = Paint().apply { isAntiAlias = true }
            paint.color = bannerColor
            val bannerRect = RectF(
                0f,
                originalBitmap.height.toFloat(),
                originalBitmap.width.toFloat(),
                (originalBitmap.height + bannerHeight).toFloat()
            )
            canvas.drawRect(bannerRect, paint)

            // 2. صياغة العنوان والتفاصيل المدمجة بين القطاع والترخيص
            val sectorTitle = sectors[sectorIndex].split("(")[0].trim()
            val tierTitle = licenseTiers[tierIndex].split("(")[0].trim()
            val headerText = "$sectorTitle | $tierTitle"

            val detailText = when (tierIndex) {
                0 -> "ترخيص تجاري معتمد للأصول الإعلامية | جميع حقوق النشر والتداول محفوظة"
                1 -> "محتوى خاص مُهدى بموجب حقوق المالك الأصلي والتأثيث الرقمي"
                2 -> "محمي جنائياً وقانونياً | المالك: saifsaadsalim3@gmail.com | يمنع التداول"
                else -> "ترخيص عام مجاني | موثق رقمياً عبر منظومة OmniLens Engine"
            }

            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val sha256Hash = generateSHA256("$timeStamp-$sectorIndex-$tierIndex-${originalBitmap.width}")

            // 3. كتابة النصوص داخل الشريط السفلي
            val textPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                textSize = bannerHeight * 0.22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val startX = 35f
            var startY = originalBitmap.height + (bannerHeight * 0.30f)

            canvas.drawText(headerText, startX, startY, textPaint)

            textPaint.textSize = bannerHeight * 0.15f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            startY += bannerHeight * 0.25f
            canvas.drawText(detailText, startX, startY, textPaint)

            textPaint.textSize = bannerHeight * 0.12f
            textPaint.color = Color.parseColor("#E2E8F0")
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

    private fun checkPermissionAndOpenCamera(isFront: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            openCamera(isFront)
        }
    }

    private fun openCamera(isFront: Boolean) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (isFront) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            intent.putExtra("useFrontCamera", true)
        } else {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 0)
        }

        try {
            val photoFile = File.createTempFile("omnilens_capture_${System.currentTimeMillis()}", ".jpg", cacheDir)
            cameraTempUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraTempUri)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, REQUEST_CAMERA_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الكاميرا: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(intent, REQUEST_GALLERY_PICK)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح المعرض: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "تم منح إذن الكاميرا، اضغط مجدداً للالتقاط", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_GALLERY_PICK -> {
                    data?.data?.let { uri ->
                        currentImageUri = uri
                        promptSectorSelection()
                    }
                }
                REQUEST_CAMERA_CAPTURE -> {
                    cameraTempUri?.let { uri ->
                        currentImageUri = uri
                        promptSectorSelection()
                    }
                }
            }
        }
    }
}
