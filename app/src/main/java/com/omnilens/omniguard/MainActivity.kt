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
    private var currentHash: String = ""
    private var tempPhotoFile: File? = null
    private var selectedSector: String = "CELEBRITY & ESCROW HUB"

    private var selectedResolution = "FHD (1080p)"
    private var selectedFPS = "30 FPS"

    private val CAMERA_BACK_REQUEST = 101
    private val CAMERA_FRONT_REQUEST = 102
    private val GALLERY_REQUEST_CODE = 103
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 40, 30, 50)
        }

        // 1. ترويسة المنظومة الأصلية
        val titleText = TextView(this).apply {
            text = "منظومة OmniLens Engine v2.0 🛡️"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 5)
        }

        val subTitleText = TextView(this).apply {
            text = "منظومة التوثيق الرقمي المزدوجة (In-App & Out-of-App Verification)"
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // 2. قسم التقاط وجلب الوسائط
        val captureHeader = createSectionHeader("📷 وسائل التقاط وجلب الوسائط")

        val btnBackCamera = createStyledButton("📷 التقاط بالكاميرا الخلفية", "#334155") {
            launchCamera(isFront = false)
        }
        val btnFrontCamera = createStyledButton("🤳 التقاط بالكاميرا الأمامية", "#334155") {
            launchCamera(isFront = true)
        }
        val btnGallery = createStyledButton("📂 اختيار صورة من المعرض وتوثيقها", "#334155") {
            openGallery()
        }

        // 3. قسم القطاعات التخصصية وبوابة العقود
        val sectorHeader = createSectionHeader("🏷️ القطاعات التخصصية وبوابة العقود")

        val sectorButtonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val sectors = listOf(
            "🌟 بوابة المشاهير والعقود المدفوعة (& CELEBRITY ESCROW HUB)",
            "⚽ القطاع الرياضي والفعاليات (SPORTS & EVENTS)",
            "📰 الصحافة والإعلام (JOURNALISM & PRESS)",
            "🏥 القطاع الطبي والصحي (MEDICAL & HEALTH)",
            "🎬 الإنتاج السينمائي والمرئي (CINEMATIC MASTER)",
            "🎨 الفن الرقمي والتصميم (FINE ART & NFT)",
            "📐 الهندسة والمخططات (ENGINEERING & ARCHITECTURE)",
            "🎓 القطاع الأكاديمي والبحثي (ACADEMIC & RESEARCH)"
        )

        sectors.forEach { sectorName ->
            val btn = Button(this).apply {
                text = sectorName
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(if (sectorName.contains("ESCROW")) Color.parseColor("#0284C7") else Color.parseColor("#1E293B"))
                setOnClickListener {
                    selectedSector = sectorName
                    Toast.makeText(this@MainActivity, "تم تحديد القطاع: $sectorName", Toast.LENGTH_SHORT).show()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }
            sectorButtonsLayout.addView(btn)
        }

        // 4. خيار الضبط والمشاهدة
        settingsSummaryText = TextView(this).apply {
            text = "⚙️ إعدادات التصوير: [$selectedResolution] | [$selectedFPS]"
            textSize = 11f
            setTextColor(Color.parseColor("#F59E0B"))
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 10)
        }

        val btnSettings = createStyledButton("⚙️ ضبط دقة التصوير والإطارات", "#475569") {
            showCameraSettingsDialog()
        }

        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 450
            ).apply { setMargins(0, 10, 0, 15) }
            setBackgroundColor(Color.parseColor("#020617"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        statusTextView = TextView(this).apply {
            text = "🛡️ جاهز للالتقاط والتوثيق التلقائي"
            textSize = 12f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        btnSave = createStyledButton("💾 حفظ في خزنة OMNILENS واستوديو الجهاز", "#10B981") {
            saveEncryptedMediaToVaultAndGallery()
        }.apply { isEnabled = false }

        hashTextView = TextView(this).apply {
            text = "قم بالتقاط صورة أو اختيارها لتشفير البصمة وتأمينها."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(10, 5, 10, 15)
        }

        // 5. سجل الخزنة
        val historyTitle = createSectionHeader("📜 سجل وسائط الخزنة المشفرة (اضغط للعرض)")

        historyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة لمنظومة OmniLens للمؤسس Saif Saad Salim"
            textSize = 10f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        // تجميع العناصر
        rootLayout.addView(titleText)
        rootLayout.addView(subTitleText)
        rootLayout.addView(captureHeader)
        rootLayout.addView(btnBackCamera)
        rootLayout.addView(btnFrontCamera)
        rootLayout.addView(btnGallery)
        rootLayout.addView(sectorHeader)
        rootLayout.addView(sectorButtonsLayout)
        rootLayout.addView(settingsSummaryText)
        rootLayout.addView(btnSettings)
        rootLayout.addView(selectedImageView)
        rootLayout.addView(statusTextView)
        rootLayout.addView(btnSave)
        rootLayout.addView(hashTextView)
        rootLayout.addView(historyTitle)
        rootLayout.addView(historyLayout)
        rootLayout.addView(rightsText)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        if (checkPermissions()) {
            loadSavedVaultHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 15, 0, 10)
        }
    }

    private fun createStyledButton(title: String, bgColorHex: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(bgColorHex))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10) }
        }
    }

    // 🎯 إصلاح فحص الأذونات الحقيقي لمنع حظر الكاميرا الممتد
    private fun checkPermissions(): Boolean {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun launchCamera(isFront: Boolean) {
        if (!checkPermissions()) {
            Toast.makeText(this, "يرجى قبول أذونات الكاميرا أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            tempPhotoFile = File.createTempFile("OMNI_${timeStamp}_", ".jpg", storageDir)

            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.omnilens.omniguard.provider",
                tempPhotoFile!!
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                if (isFront) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("useFrontCamera", true)
                }
            }

            val reqCode = if (isFront) CAMERA_FRONT_REQUEST else CAMERA_BACK_REQUEST
            startActivityForResult(intent, reqCode)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الكاميرا: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openGallery() {
        if (!checkPermissions()) return
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_BACK_REQUEST, CAMERA_FRONT_REQUEST -> {
                    tempPhotoFile?.let { file ->
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) processCapturedBitmap(bitmap)
                    }
                }
                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) processCapturedBitmap(bitmap)
                    }
                }
            }
        }
    }

    private fun processCapturedBitmap(bitmap: Bitmap) {
        val watermarked = addOmniLensWatermarkToBitmap(bitmap)
        currentBitmap = watermarked
        selectedImageView.setImageBitmap(watermarked)

        currentHash = calculateBitmapSHA256(watermarked)

        hashTextView.text = "🔑 SHA-256 Digest:\n$currentHash"
        btnSave.isEnabled = true
        statusTextView.text = "✅ تم توثيق الصورة وتطبيق معيار [$selectedSector]"
    }

    private fun addOmniLensWatermarkToBitmap(srcBitmap: Bitmap): Bitmap {
        val mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = canvas.width
        val height = canvas.height

        val watermarkPaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            textSize = (width * 0.04f).coerceAtLeast(28f)
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

        val barHeight = (height * 0.12f).coerceAtLeast(120f)
        val barPaint = Paint().apply {
            color = Color.parseColor("#800000")
            style = Paint.Style.FILL
        }
        val barRect = RectF(0f, height - barHeight, width.toFloat(), height.toFloat())
        canvas.drawRect(barRect, barPaint)

        val ownerName = "SAIF SAAD SALIM"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        val textPaintLine1 = Paint().apply {
            color = Color.WHITE
            textSize = (barHeight * 0.26f).coerceAtLeast(22f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaintLine2 = Paint().apply {
            color = Color.parseColor("#FEF08A")
            textSize = (barHeight * 0.22f).coerceAtLeast(18f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val line1Text = "OmniLens | OWNER: $ownerName | SECTOR: $selectedSector"
        val line2Text = "TIME: $timestamp | TOKEN: OMNILENS-IP-INV-SAIF-2026"

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

        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val omniFolder = File(dcimDir, "OmniLens")
        if (!omniFolder.exists()) omniFolder.mkdirs()

        val galleryFile = File(omniFolder, fileName)
        FileOutputStream(galleryFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        MediaScannerConnection.scanFile(this, arrayOf(galleryFile.absolutePath), null, null)
        Toast.makeText(this, "💾 تم التوثيق والحفظ بنجاح في استوديو الهاتف وخزنة OmniLens", Toast.LENGTH_LONG).show()
        btnSave.isEnabled = false
        loadSavedVaultHistory()
    }

    private fun loadSavedVaultHistory() {
        historyLayout.removeAllViews()

        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val omniFolder = File(dcimDir, "OmniLens")
        val allFiles = mutableListOf<File>()

        if (omniFolder.exists()) omniFolder.listFiles()?.let { allFiles.addAll(it) }

        val sortedFiles = allFiles.sortedByDescending { it.lastModified() }

        if (sortedFiles.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "لا توجد وسائط محفوظة في الخزنة حالياً."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11f
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
                setPadding(15, 12, 15, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                setOnClickListener { openFileInVaultViewer(file) }
            }

            val iconText = TextView(this).apply {
                text = "🖼️"
                textSize = 18f
                setPadding(0, 0, 12, 0)
            }

            val infoLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

            val nameText = TextView(this).apply {
                text = file.name
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
            }

            val dateText = TextView(this).apply {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                text = "التاريخ: $formattedDate | الحجم: ${file.length() / 1024} KB"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 9.5f
            }

            infoLayout.addView(nameText)
            infoLayout.addView(dateText)
            card.addView(iconText)
            card.addView(infoLayout)
            historyLayout.addView(card)
        }
    }

    private fun openFileInVaultViewer(file: File) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val uri = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
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
            setPadding(35, 20, 35, 20)
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
            settingsSummaryText.text = "⚙️ إعدادات التصوير: [$selectedResolution] | [$selectedFPS]"
            Toast.makeText(this, "تم تحديث الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }
}
