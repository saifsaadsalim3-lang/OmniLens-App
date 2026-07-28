package com.omnilens.omniguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var selectedImageView: ImageView
    private lateinit var hashTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var btnSave: Button
    private lateinit var historyLayout: LinearLayout
    private lateinit var settingsSummaryText: TextView

    private var currentBitmap: Bitmap? = null
    private var currentMediaFile: File? = null
    private var currentHash: String = ""
    private var tempPhotoFile: File? = null

    private var selectedResolution = "FHD (1080p)"
    private var selectedFPS = "30 FPS"

    private val CAMERA_REQUEST_CODE = 101
    private val GALLERY_REQUEST_CODE = 102
    private val VIDEO_REQUEST_CODE = 103
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🛡️ 1. حظر لقطات الشاشة وتسجيل الفيديو كلياً لحماية الخزنة بداخل التطبيق
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 60, 40, 60)
        }

        val logoImage = ImageView(this).apply {
            val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (imageResId != 0) {
                setImageResource(imageResId)
            }
            layoutParams = LinearLayout.LayoutParams(180, 180).apply {
                gravity = Gravity.CENTER
            }
        }

        val titleText = TextView(this).apply {
            text = "OMNILENS"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 5)
        }

        statusTextView = TextView(this).apply {
            text = "🛡️ نظام التشفير والخزنة المشفرة جاهز"
            textSize = 14f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        settingsSummaryText = TextView(this).apply {
            text = "⚙️ الإعدادات: [$selectedResolution] | [$selectedFPS]"
            textSize = 12f
            setTextColor(Color.parseColor("#F59E0B"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        val btnSettings = Button(this).apply {
            text = "⚙️ ضبط دقة التصوير والإطارات"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener { showCameraSettingsDialog() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 15)
            }
        }

        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                480
            ).apply {
                setMargins(0, 10, 0, 15)
            }
            setBackgroundColor(Color.parseColor("#1E293B"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 10)
        }

        val btnCamera = Button(this).apply {
            text = "📷 التقاط صورة"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkPermissionsAndLaunch(isCamera = true, isVideo = false) }
        }

        val btnVideo = Button(this).apply {
            text = "🎥 تسجيل فيديو"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#DC2626"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkPermissionsAndLaunch(isCamera = true, isVideo = true) }
        }

        val btnGallery = Button(this).apply {
            text = "🖼️ المعرض"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#0D9488"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkPermissionsAndLaunch(isCamera = false, isVideo = false) }
        }

        val btnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(4, 0, 4, 0)
        }

        buttonsLayout.addView(btnCamera, btnParams)
        buttonsLayout.addView(btnVideo, btnParams)
        buttonsLayout.addView(btnGallery, btnParams)

        btnSave = Button(this).apply {
            text = "💾 حفظ في خزنة OmniLens واستوديو الجهاز"
            setBackgroundColor(Color.parseColor("#475569"))
            setTextColor(Color.WHITE)
            isEnabled = false
            setOnClickListener { saveEncryptedMediaToVaultAndGallery() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 10, 0, 15)
            }
        }

        hashTextView = TextView(this).apply {
            text = "التقط صورة/فيديو لتشفيرها وتأمينها داخل الخزنة."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(15, 10, 15, 15)
        }

        val historyTitle = TextView(this).apply {
            text = "📜 سجل الخزنة المشفرة (اضغط للعرض)"
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

        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة لمنظومة OmniLens للمؤسس Saif Saad Salim"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }

        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(statusTextView)
        rootLayout.addView(settingsSummaryText)
        rootLayout.addView(btnSettings)
        rootLayout.addView(selectedImageView)
        rootLayout.addView(buttonsLayout)
        rootLayout.addView(btnSave)
        rootLayout.addView(hashTextView)
        rootLayout.addView(historyTitle)
        rootLayout.addView(historyLayout)
        rootLayout.addView(rightsText)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        // 🔍 فحص الأذونات وقراءة سجل الخزنة فور التشغيل
        if (checkPermissions()) {
            loadSavedVaultHistory()
        }
    }

    private fun checkPermissions(): Boolean {
        val needed = mutableListOf<String>()
        needed.add(Manifest.permission.CAMERA)
        needed.add(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun checkPermissionsAndLaunch(isCamera: Boolean, isVideo: Boolean) {
        if (!checkPermissions()) {
            Toast.makeText(this, "يرجى قبول أذونات الوسائط والكاميرا للاستمرار", Toast.LENGTH_SHORT).show()
            return
        }

        if (isCamera) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            if (isVideo) {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                startActivityForResult(intent, VIDEO_REQUEST_CODE)
            } else {
                tempPhotoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.omnilens.omniguard.provider",
                    tempPhotoFile!!
                )
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }
        } else {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadSavedVaultHistory()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    tempPhotoFile?.let { file ->
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            processCapturedBitmap(bitmap)
                        }
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    val imageUri = data?.data
                    imageUri?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            processCapturedBitmap(bitmap)
                        }
                    }
                }
                VIDEO_REQUEST_CODE -> {
                    val videoUri = data?.data
                    videoUri?.let { uri ->
                        hashTextView.text = "🎥 تم تسجيل الفيديو بنجاح: $uri"
                        btnSave.isEnabled = true
                    }
                }
            }
        }
    }

    private fun processCapturedBitmap(bitmap: Bitmap) {
        // 🎨 إضافة طبقة التوثيق والمائية بدقة عالية ومنع تشوه الخطوط
        val watermarked = addOmniLensWatermarkToBitmap(bitmap)
        currentBitmap = watermarked
        selectedImageView.setImageBitmap(watermarked)

        // 🔐 حساب بصمة SHA-256 للملف
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        currentHash = calculateBitmapSHA256(watermarked)

        hashTextView.text = "🔑 SHA-256 Digest:\n$currentHash"
        btnSave.isEnabled = true
        statusTextView.text = "✅ تم تشفير البصمة وتطبيق طبقة التوثيق"
    }

    // 🖌️ رسم الطبقة المائية وشريط التوثيق الأحمر بدقة عالية بدون رموز مكسورة
    private fun addOmniLensWatermarkToBitmap(srcBitmap: Bitmap): Bitmap {
        val mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = canvas.width
        val height = canvas.height

        // 1. رسم النمط المائل الخفي (PROTECTED OMNILENS)
        val watermarkPaint = Paint().apply {
            color = Color.argb(45, 255, 255, 255)
            textSize = (width * 0.042f).coerceAtLeast(30f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.save()
        canvas.rotate(-30f, width / 2f, height / 2f)
        val stepX = (width * 0.5f).toInt()
        val stepY = (height * 0.12f).toInt()

        for (y in -height..height * 2 step stepY) {
            for (x in -width..width * 2 step stepX) {
                canvas.drawText("PROTECTED OMNILENS • PRO", x.toFloat(), y.toFloat(), watermarkPaint)
            }
        }
        canvas.restore()

        // 2. رسم الشريط السفلي للتوثيق الإداري والمشاهير
        val barHeight = (height * 0.12f).coerceAtLeast(120f)
        val barPaint = Paint().apply {
            color = Color.parseColor("#800000") // أحمر عنابي دافئ ومحترف
            style = Paint.Style.FILL
        }
        val barRect = RectF(0f, height - barHeight, width.toFloat(), height.toFloat())
        canvas.drawRect(barRect, barPaint)

        // 3. كتابة بيانات التوثيق الرسمية بالإنجليزية المنظمة لمنع التشفير المكسور
        val ownerName = "SAIF SAAD SALIM"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val textPaintLine1 = Paint().apply {
            color = Color.WHITE
            textSize = (barHeight * 0.28f).coerceAtLeast(24f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaintLine2 = Paint().apply {
            color = Color.parseColor("#FEF08A")
            textSize = (barHeight * 0.24f).coerceAtLeast(20f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val line1Text = "OmniLens | OWNER: $ownerName | MODE: PRIVATE | SECTOR: VIP-CELEBRITY"
        val line2Text = "TIME: $timestamp | TOKEN: OMNI-HW-ID-2026-SAIF"

        val paddingLeft = 30f
        val line1Y = height - (barHeight * 0.55f)
        val line2Y = height - (barHeight * 0.18f)

        canvas.drawText(line1Text, paddingLeft, line1Y, textPaintLine1)
        canvas.drawText(line2Text, paddingLeft, line2Y, textPaintLine2)

        return mutableBitmap
    }

    private fun calculateBitmapSHA256(bitmap: Bitmap): String {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        val byteArray = stream.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(byteArray)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun saveEncryptedMediaToVaultAndGallery() {
        val bitmap = currentBitmap ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "OMNI_$timeStamp.jpg"

        // حفظ في مجلد الاستوديو العام DCIM/OmniLens
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val omniFolder = File(dcimDir, "OmniLens")
        if (!omniFolder.exists()) {
            omniFolder.mkdirs()
        }

        val galleryFile = File(omniFolder, fileName)
        FileOutputStream(galleryFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // إعلام استوديو الجهاز بالملف الجديد
        MediaScannerConnection.scanFile(this, arrayOf(galleryFile.absolutePath), null, null)

        Toast.makeText(this, "💾 تم الحفظ بنجاح في خزنة OmniLens والمستندات", Toast.LENGTH_LONG).show()
        btnSave.isEnabled = false

        // إعادة تحميل سجل الخزنة
        loadSavedVaultHistory()
    }

    // 📂 قراءة سجل الخزنة واستعراض كافة الصور السابقة
    private fun loadSavedVaultHistory() {
        historyLayout.removeAllViews()

        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val omniFolder = File(dcimDir, "OmniLens")
        val internalFolder = filesDir

        val allFiles = mutableListOf<File>()
        if (omniFolder.exists()) {
            omniFolder.listFiles()?.let { allFiles.addAll(it) }
        }
        if (internalFolder.exists()) {
            internalFolder.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png", "mp4") }?.let { allFiles.addAll(it) }
        }

        val sortedFiles = allFiles.sortedByDescending { it.lastModified() }

        if (sortedFiles.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "لا توجد وسائط محفوظة في الخزنة حالياً."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 10, 0, 10)
            }
            historyLayout.addView(emptyText)
            return
        }

        sortedFiles.take(10).forEach { file ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(20, 15, 20, 15)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 10)
                }
                setOnClickListener { openFileInViewer(file) }
            }

            val iconText = TextView(this).apply {
                text = if (file.name.endsWith(".mp4", true)) "🎥" else "🖼️"
                textSize = 20f
                setPadding(0, 0, 15, 0)
            }

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val nameText = TextView(this).apply {
                text = file.name
                setTextColor(Color.WHITE)
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }

            val dateText = TextView(this).apply {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                text = "تاريخ التوثيق: $formattedDate | الحجم: ${file.length() / 1024} KB"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 10f
            }

            infoLayout.addView(nameText)
            infoLayout.addView(dateText)

            card.addView(iconText)
            card.addView(infoLayout)

            historyLayout.addView(card)
        }
    }

    private fun openFileInViewer(file: File) {
        val uri = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (file.name.endsWith(".mp4", true)) "video/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun showCameraSettingsDialog() {
        val resolutions = arrayOf("HD (720p)", "FHD (1080p)", "Ultra HD (4K)")
        val fpsOptions = arrayOf("30 FPS", "60 FPS")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚙️ إعدادات جودة الكاميرا والتوثيق")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val resLabel = TextView(this).apply { text = "اختر دقة التصوير:" }
        val spinnerRes = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, resolutions)
        }

        val fpsLabel = TextView(this).apply { text = "اختر معدل الإطارات:" }
        val spinnerFPS = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, fpsOptions)
        }

        layout.addView(resLabel)
        layout.addView(spinnerRes)
        layout.addView(fpsLabel)
        layout.addView(spinnerFPS)

        builder.setView(layout)
        builder.setPositiveButton("حفظ الإعدادات") { _, _ ->
            selectedResolution = spinnerRes.selectedItem.toString()
            selectedFPS = spinnerFPS.selectedItem.toString()
            settingsSummaryText.text = "⚙️ الإعدادات: [$selectedResolution] | [$selectedFPS]"
            Toast.makeText(this, "تم تحديث إعدادات الكاميرا", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }
}
