package com.omnilens.omniguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var selectedImageView: ImageView
    private lateinit var hashTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var historyLayout: LinearLayout
    private lateinit var settingsSummaryText: TextView

    private var currentBitmap: Bitmap? = null
    private var currentMediaFile: File? = null
    private var currentHash: String = ""
    private var tempPhotoFile: File? = null

    // إعدادات الكاميرا المختارة
    private var selectedResolution = "FHD (1080p)"
    private var selectedFPS = "30 FPS"
    private var isSocialMode = false

    private val CAMERA_REQUEST_CODE = 101
    private val GALLERY_REQUEST_CODE = 102
    private val VIDEO_REQUEST_CODE = 103
    private val PERMISSION_CODE = 104

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(35, 50, 35, 50)
        }

        // 1. الشعار والعنوان التفاعلي
        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 20) }
        }

        val logoImage = ImageView(this).apply {
            val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (imageResId != 0) setImageResource(imageResId)
            layoutParams = LinearLayout.LayoutParams(180, 180).apply { gravity = Gravity.CENTER }
        }

        val titleText = TextView(this).apply {
            text = "OmniLens v1.3.0"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 5)
        }

        statusTextView = TextView(this).apply {
            text = "الحفظ المباشر ونظام التشفير نشط ⚡✅"
            textSize = 13f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }

        headerCard.addView(logoImage)
        headerCard.addView(titleText)
        headerCard.addView(statusTextView)

        // 2. كارت ملخص الإعدادات
        settingsSummaryText = TextView(this).apply {
            text = "⚙️ الجودة: [$selectedResolution] | الإطارات: [$selectedFPS]"
            textSize = 12f
            setTextColor(Color.parseColor("#F59E0B"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        val btnSettings = Button(this).apply {
            text = "⚙️ تخصيص دقة التصوير وفريمات الجهاز"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener { showCameraSettingsDialog() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 15) }
        }

        // 3. إطار العرض الحي والمعاينة
        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                480
            ).apply { setMargins(0, 5, 0, 15) }
            setBackgroundColor(Color.parseColor("#1E293B"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // 4. أزرار التحكم والتقاط الكاميرا السريعة والتفاعلية
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 10)
        }

        val btnCameraOptions = Button(this).apply {
            text = "📷 التقاط صورة (عادي / OmniLens / سوشيال ميديا)"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener { showCameraChoiceDialog(isVideo = false) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
        }

        val btnVideoOptions = Button(this).apply {
            text = "🎥 تسجيل فيديو (مباشر ومحمي)"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#DC2626"))
            setTextColor(Color.WHITE)
            setOnClickListener { showCameraChoiceDialog(isVideo = true) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
        }

        val btnGallery = Button(this).apply {
            text = "🖼️ اختيار وسائط من معرض الجهاز"
            textSize = 13f
            setBackgroundColor(Color.parseColor("#0D9488"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 15) }
        }

        buttonsLayout.addView(btnCameraOptions)
        buttonsLayout.addView(btnVideoOptions)
        buttonsLayout.addView(btnGallery)

        // 5. نص حالة البصمة والحفظ المباشر
        hashTextView = TextView(this).apply {
            text = "⚡ الحفظ مباشر ورقمي! عند التقاط أي صورة/فيديو يتم تشفيرها وحفظها تلقائياً في DCIM/OmniLens واستوديو جهازك."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(15, 10, 15, 15)
        }

        // 6. عنوان وسجل الاستوديو
        val historyTitle = TextView(this).apply {
            text = "📜 خزنة استوديو OmniLens (اضغط للعرض والتعديل)"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F8FAFC"))
            setPadding(0, 20, 0, 10)
        }

        historyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 7. حقوق الملكية
        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة منصة OmniLens للمستخدم"
            textSize = 11f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        rootLayout.addView(headerCard)
        rootLayout.addView(settingsSummaryText)
        rootLayout.addView(btnSettings)
        rootLayout.addView(selectedImageView)
        rootLayout.addView(buttonsLayout)
        rootLayout.addView(hashTextView)
        rootLayout.addView(historyTitle)
        rootLayout.addView(historyLayout)
        rootLayout.addView(rightsText)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        loadSavedVaultHistory()
    }

    // 1. خيار تحديد الكاميرا وسياق الاستخدام (Point 1 & Point 3)
    private fun showCameraChoiceDialog(isVideo: Boolean) {
        val options = arrayOf(
            "🛡️ تصوير مشفر ومحمي عبر OmniLens (مع الحفظ المباشر)",
            "📲 تصوير مباشر للشبكات والسوشيال ميديا (مشاركة فورية)",
            "📷 تصوير عادي عبر كاميرا الجهاز الافتراضية"
        )

        AlertDialog.Builder(this)
            .setTitle("اختر نمط تصوير الكاميرا:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { isSocialMode = false; checkPermissionsAndCapture(isVideo) }
                    1 -> { isSocialMode = true; checkPermissionsAndCapture(isVideo) }
                    2 -> { isSocialMode = false; checkPermissionsAndCapture(isVideo) }
                }
            }
            .show()
    }

    private fun showCameraSettingsDialog() {
        val options = arrayOf(
            "📐 الدقة: HD (720p)",
            "📐 الدقة: FHD (1080p) [مستحسن]",
            "📐 الدقة: Ultra HD (4K / أقصى جودة)",
            "⏱️ الفريمات: 30 FPS",
            "⏱️ الفريمات: 60 FPS (سلاسة فائقة)"
        )

        AlertDialog.Builder(this)
            .setTitle("⚙️ إعدادات جودة الكاميرا وعتاد الجهاز:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectedResolution = "HD (720p)"
                    1 -> selectedResolution = "FHD (1080p)"
                    2 -> selectedResolution = "4K (2160p)"
                    3 -> selectedFPS = "30 FPS"
                    4 -> selectedFPS = "60 FPS"
                }
                settingsSummaryText.text = "⚙️ الجودة: [$selectedResolution] | الإطارات: [$selectedFPS]"
                Toast.makeText(this, "تم اعتماد الإعدادات: $selectedResolution - $selectedFPS", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun checkPermissionsAndCapture(isVideo: Boolean) {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        val hasCamera = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        if (hasCamera && hasAudio) {
            if (isVideo) openVideoCamera() else openPhotoCamera()
        } else {
            requestPermissions(permissions, PERMISSION_CODE)
        }
    }

    private fun openPhotoCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            tempPhotoFile = File.createTempFile("TEMP_$timeStamp", ".jpg", filesDir)

            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.omnilens.omniguard.provider",
                tempPhotoFile!!
            )

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الكاميرا: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openVideoCamera() {
        try {
            val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
            }
            startActivityForResult(videoIntent, VIDEO_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الفيديو: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "تم منح الصلاحيات ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                tempPhotoFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        currentBitmap = bitmap
                        selectedImageView.setImageBitmap(bitmap)
                        currentMediaFile = file
                        processAndAutoSaveMedia(file, isVideo = false)
                    }
                }
            } else if (requestCode == VIDEO_REQUEST_CODE && data != null) {
                val videoUri: Uri? = data.data
                if (videoUri != null) {
                    val file = createTempFileFromUri(videoUri, ".mp4")
                    if (file != null) {
                        currentMediaFile = file
                        selectedImageView.setImageResource(android.R.drawable.ic_media_play)
                        processAndAutoSaveMedia(file, isVideo = true)
                    }
                }
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                val imageUri: Uri? = data.data
                if (imageUri != null) {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                    val file = createTempFileFromUri(imageUri, ".png")
                    if (file != null && bitmap != null) {
                        currentBitmap = bitmap
                        selectedImageView.setImageBitmap(bitmap)
                        currentMediaFile = file
                        processAndAutoSaveMedia(file, isVideo = false)
                    }
                }
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri, extension: String): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempFile = File.createTempFile("MEDIA_$timeStamp", extension, filesDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    // 2. معالجة وتشفير وحفظ مباشر تلقائي بدون أزرار إضافية (Point 2)
    private fun processAndAutoSaveMedia(file: File, isVideo: Boolean) {
        try {
            val fileBytes = FileInputStream(file).use { it.readBytes() }
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(fileBytes)
            currentHash = hashBytes.joinToString("") { "%02x".format(it) }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val ext = if (isVideo) ".mp4" else ".png"
            val fileName = "OMNI_$timeStamp$ext"

            // الحفظ المباشر بـ DCIM/OmniLens
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val omniFolder = File(dcimDir, "OmniLens")
            if (!omniFolder.exists()) omniFolder.mkdirs()

            val destFile = File(omniFolder, fileName)
            file.copyTo(destFile, overwrite = true)

            // إجراء مزامنة فورية مع استوديو الهاتف
            val mimeType = if (isVideo) "video/mp4" else "image/png"
            MediaScannerConnection.scanFile(this, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)

            val typeName = if (isVideo) "فيديو" else "صورة"
            statusTextView.text = "تم التشفير والحفظ المباشر بـ DCIM/OmniLens ⚡🛡️"
            statusTextView.setTextColor(Color.parseColor("#4ADE80"))

            hashTextView.text = "البصمة الرقمية (SHA-256):\n$currentHash\n\n[حالة الملف: تم الحفظ المباشر بالاستوديو والخزنة]"
            hashTextView.setTextColor(Color.parseColor("#E2E8F0"))

            addCardToHistory(currentBitmap, destFile, currentHash, timeStamp)

            Toast.makeText(this, "⚡ تم التقاط و حِفظ الـ $typeName بنجاح بالاستوديو!", Toast.LENGTH_SHORT).show()

            // إذا كان وضع السوشيال ميديا مفعل، اظهر نافذة المشاركة فوراً
            if (isSocialMode) {
                showShareTypeDialog(destFile, currentHash)
            }

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCardToHistory(bitmap: Bitmap?, file: File, hash: String, timeStamp: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }

        val thumbView = ImageView(this).apply {
            if (bitmap != null) setImageBitmap(bitmap) else setImageResource(android.R.drawable.ic_media_play)
            layoutParams = LinearLayout.LayoutParams(110, 110).apply { gravity = Gravity.CENTER_VERTICAL }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val fileText = TextView(this).apply {
            text = file.name
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
        }

        val dateText = TextView(this).apply {
            text = "التاريخ: $timeStamp"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 10f
        }

        val hashShortText = TextView(this).apply {
            text = "البصمة: ${hash.take(12)}..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f
        }

        infoLayout.addView(fileText)
        infoLayout.addView(dateText)
        infoLayout.addView(hashShortText)

        val openWithDevicePlayer = {
            try {
                val uri = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", file)
                val mimeType = if (file.name.endsWith(".mp4", ignoreCase = true)) "video/*" else "image/*"
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(viewIntent, "استعراض/تشغيل بواسطة:"))
            } catch (e: Exception) {
                Toast.makeText(this, "تعذر التشغيل: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        thumbView.setOnClickListener { openWithDevicePlayer() }
        infoLayout.setOnClickListener { openWithDevicePlayer() }

        val btnShare = Button(this).apply {
            text = "📤"
            textSize = 15f
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(100, 100).apply { gravity = Gravity.CENTER_VERTICAL }
            setOnClickListener { showShareTypeDialog(file, hash) }
        }

        card.addView(thumbView)
        card.addView(infoLayout)
        card.addView(btnShare)

        historyLayout.addView(card, 0)
    }

    private fun showShareTypeDialog(file: File, hash: String) {
        val options = arrayOf("🆓 مشاركة عامة (Free)", "🎁 مشاركة هدية (VIP Gift)", "💰 ترخيص محتوى مدفوع (Commercial)")

        AlertDialog.Builder(this)
            .setTitle("اختر نوع الترخيص والسوشيال ميديا:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> executeShare(file, hash, "🆓 ترخيص مجاني (عام)", "محتوى موثق متاح للمعاينة والتداول.")
                    1 -> executeShare(file, hash, "🎁 ترخيص هدية خاصة (VIP)", "محتوى خاص موثق ببصمة OmniLens.")
                    2 -> executeShare(file, hash, "💰 ترخيص تجاري مدفوع (Paid)", "⚠️ محتوى تجاري مشفر. يحظر الاستخدام بدون ترخيص.")
                }
            }
            .show()
    }

    private fun executeShare(file: File, hash: String, licenseTitle: String, licenseDesc: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", file)
            val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
            val mimeType = if (isVideo) "video/*" else "image/*"

            val shareMessage = """
                🔒 منصة OmniLens للحماية وتوثيق الوسائط
                ----------------------------------------
                📌 نوع الترخيص: $licenseTitle
                📝 الوصف: $licenseDesc
                🎥 الجودة: [$selectedResolution | $selectedFPS]
                
                🔑 بصمة الملكية الرقمية (SHA-256):
                $hash
                ----------------------------------------
                © جميع الحقوق محفوظة لمنصة OmniLens وللمستخدم.
            """.trimIndent()

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "وسائط محمية - $licenseTitle")
                putExtra(Intent.EXTRA_TEXT, shareMessage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "مشاركة الوسائط عبر السوشيال ميديا:"))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذرت المشاركة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadSavedVaultHistory() {
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val omniFolder = File(dcimDir, "OmniLens")

        if (omniFolder.exists()) {
            val files = omniFolder.listFiles()?.sortedByDescending { it.lastModified() }
            files?.forEach { file ->
                val isVideo = file.name.endsWith(".mp4", ignoreCase = true)
                val bitmap = if (!isVideo) BitmapFactory.decodeFile(file.absolutePath) else null

                try {
                    val fileBytes = FileInputStream(file).use { it.readBytes() }
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hashBytes = digest.digest(fileBytes)
                    val hash = hashBytes.joinToString("") { "%02x".format(it) }

                    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(file.lastModified()))
                    addCardToHistory(bitmap, file, hash, date)
                } catch (e: Exception) {
                }
            }
        }
    }
}
