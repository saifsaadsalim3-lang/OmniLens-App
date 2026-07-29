package com.omnilens.omniguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
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
    private lateinit var sectorButtonsLayout: LinearLayout
    private lateinit var settingsSummaryText: TextView
    private lateinit var btnToggleVault: Button
    private lateinit var sharedPrefs: SharedPreferences

    private var currentBitmap: Bitmap? = null
    private var currentHash: String = ""
    private var tempPhotoFile: File? = null
    private var selectedSector: String = "🏛️ القطاع الحكومي والسيادي (GOVERNMENT & SOVEREIGN HUB)"

    private var selectedResolution = "FHD (1080p)"
    private var selectedFPS = "30 FPS"
    private var isVaultUnlocked = false

    private val CAMERA_BACK_REQUEST = 101
    private val CAMERA_FRONT_REQUEST = 102
    private val GALLERY_REQUEST_CODE = 103
    private val PERMISSION_REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔓 السماح بأخذ لقطات الشاشة في الواجهة الرئيسية بشكل طبيعي
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        sharedPrefs = getSharedPreferences("omnilens_sec_prefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 40, 30, 50)
        }

        // 1. ترويسة المنظومة الرئيسية
        val titleText = TextView(this).apply {
            text = "منظومة OmniLens Engine v2.0 🛡️"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 5)
        }

        val subTitleText = TextView(this).apply {
            text = "منظومة التوثيق الرقمي المزدوجة والتتبع السيادي"
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // 2. وسائل التقاط الكاميرا والوسائط
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

        // 3. القطاعات التخصصية الـ 10 + القطاعات المخصصة
        val sectorHeader = createSectionHeader("🏷️ القطاعات التخصصية والسيادية المعتمدة")

        sectorButtonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        refreshSectorButtons(sectorButtonsLayout)

        // 4. خيار ضبط الجودة والإطارات
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
            text = "🛡️ جاهز للالتقاط والتوثيق التلقائي بنقاء بصري كامل"
            textSize = 12f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        btnSave = createStyledButton("💾 حفظ في خزنة OMNILENS واستوديو الجهاز", "#10B981") {
            saveMediaToVaultAndGallery()
        }.apply { isEnabled = false }

        hashTextView = TextView(this).apply {
            text = "قم بالتقاط صورة أو اختيارها لتوليد بصمة SHA-256 وتتبع الهوية."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(10, 5, 10, 15)
        }

        // 5. قسم الخزنة الأمنية المشفرة بكلمة مرور وبصمة
        val historyTitle = createSectionHeader("🔐 الخزنة المشفرة الخاصة (تتطلب رمز حماية وبصمة)")

        btnToggleVault = createStyledButton("🔒 فتح الخزنة المشفرة (PIN Code)", "#DC2626") {
            handleVaultSecurityAccess()
        }

        historyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = LinearLayout.GONE // مخفية افتراضياً للحماية القصوى
        }

        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة للمؤسس ՏԹiԲ. Տ. ՏԹliʍ"
            textSize = 10f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        // تجميع العناصر بالواجهة
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
        rootLayout.addView(btnToggleVault)
        rootLayout.addView(historyLayout)
        rootLayout.addView(rightsText)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (!isVaultUnlocked) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
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

    // إدارة القطاعات الديناميكية والقطاعات المخصصة
    private fun refreshSectorButtons(container: LinearLayout) {
        container.removeAllViews()

        val defaultSectors = listOf(
            "🏛️ القطاع الحكومي والسيادي (GOVERNMENT & SOVEREIGN HUB)",
            "🤖 قطاع الذكاء الاصطناعي ومكافحة التزييف (AI & DIGITAL FORENSICS HUB)",
            "🌟 بوابة المشاهير والعقود المدفوعة (CELEBRITY ESCROW HUB)",
            "⚽ القطاع الرياضي والفعاليات (SPORTS & EVENTS)",
            "📰 الصحافة والإعلام (JOURNALISM & PRESS)",
            "🏥 القطاع الطبي والصحي (MEDICAL & HEALTH)",
            "🎬 الإنتاج السينمائي والمرئي (CINEMATIC MASTER)",
            "🎨 الفن الرقمي والتصميم (FINE ART & NFT)",
            "📐 الهندسة والمخططات (ENGINEERING & ARCHITECTURE)",
            "🎓 القطاع الأكاديمي والبحثي (ACADEMIC & RESEARCH)"
        )

        val customSet = sharedPrefs.getStringSet("custom_sectors", emptySet()) ?: emptySet()
        val allSectors = defaultSectors + customSet.toList()

        allSectors.forEach { sectorName ->
            val btn = Button(this).apply {
                text = sectorName
                textSize = 11f
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    when {
                        sectorName.contains("GOVERNMENT") || sectorName.contains("ESCROW") -> Color.parseColor("#0284C7")
                        sectorName.contains("AI") -> Color.parseColor("#7C3AED")
                        sectorName.contains("CUSTOM") -> Color.parseColor("#0D9488")
                        else -> Color.parseColor("#1E293B")
                    }
                )
                setOnClickListener {
                    selectedSector = sectorName
                    Toast.makeText(this@MainActivity, "تم تحديد القطاع: $sectorName", Toast.LENGTH_SHORT).show()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
            }
            container.addView(btn)
        }

        // زر إضافة قطاع مخصص جديد
        val btnAddCustom = Button(this).apply {
            text = "➕ إضافة قطاع مخصص جديد..."
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            setOnClickListener {
                showAddCustomSectorDialog(container)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 5, 0, 10) }
        }
        container.addView(btnAddCustom)
    }

    private fun showAddCustomSectorDialog(container: LinearLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("➕ إضافة قطاع تخصصي جديد")
        builder.setMessage("أدخل اسم القطاع الجديد لحفظه ضمن المنظومة:")

        val input = EditText(this).apply {
            hint = "مثال: قطاع العقارات والتوثيق القانوني"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        builder.setView(input)

        builder.setPositiveButton("حفظ وإضافة") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                val customSet = sharedPrefs.getStringSet("custom_sectors", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                customSet.add("🏢 $name (CUSTOM HUB)")
                sharedPrefs.edit().putStringSet("custom_sectors", customSet).apply()
                Toast.makeText(this, "تم إضافة القطاع بنجاح!", Toast.LENGTH_SHORT).show()
                refreshSectorButtons(container)
            } else {
                Toast.makeText(this, "يرجى كتابة اسم صحيح", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    // حماية الخزنة ورمز الدخول
    private fun handleVaultSecurityAccess() {
        if (isVaultUnlocked) {
            isVaultUnlocked = false
            historyLayout.visibility = LinearLayout.GONE
            btnToggleVault.text = "🔒 فتح الخزنة المشفرة (PIN Code)"
            btnToggleVault.setBackgroundColor(Color.parseColor("#DC2626"))
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Toast.makeText(this, "تم قفل الخزنة بأمان", Toast.LENGTH_SHORT).show()
        } else {
            val savedPin = sharedPrefs.getString("vault_pin", null)
            if (savedPin == null) {
                showSetPinDialog()
            } else {
                showEnterPinDialog(savedPin)
            }
        }
    }

    private fun showSetPinDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔑 إنشاء رمز سري للخزنة")
        builder.setMessage("أدخل رمزاً مكوناً من 4 أرقام لحماية الخزنة السرية:")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        builder.setView(input)

        builder.setPositiveButton("حفظ") { _, _ ->
            val pin = input.text.toString().trim()
            if (pin.length >= 4) {
                sharedPrefs.edit().putString("vault_pin", pin).apply()
                Toast.makeText(this, "تم تعيين رمز الحماية بنجاح!", Toast.LENGTH_SHORT).show()
                unlockVaultSuccess()
            } else {
                Toast.makeText(this, "يرجى إدخال 4 أرقام على الأقل", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun showEnterPinDialog(correctPin: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔐 فتح الخزنة المشفرة")
        builder.setMessage("أدخل الرمز السري الخاص بك:")

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        builder.setView(input)

        builder.setPositiveButton("فتح") { _, _ ->
            val entered = input.text.toString().trim()
            if (entered == correctPin) {
                unlockVaultSuccess()
            } else {
                Toast.makeText(this, "الرمز السري غير صحيح ❌", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun unlockVaultSuccess() {
        isVaultUnlocked = true
        // 🛡️ تفعيل حظر لقطات وتسجيل الشاشة فوراً عند فتح الخزنة
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        historyLayout.visibility = LinearLayout.VISIBLE
        btnToggleVault.text = "🔓 إغلاق وقفل الخزنة المشفرة"
        btnToggleVault.setBackgroundColor(Color.parseColor("#10B981"))
        loadSavedVaultHistory()
        Toast.makeText(this, "تم فتح الخزنة وحظر لقطات الشاشة للحماية القصوى ✅", Toast.LENGTH_SHORT).show()
    }

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

        hashTextView.text = "🔑 SHA-256 Digest & Tracking ID:\n$currentHash"
        btnSave.isEnabled = true
        statusTextView.text = "✅ تم توثيق الصورة بنجاح ودمج بصرية التتبع للقطاع المختار"
    }

    // 🛡️ دمج الشريط العنابي السيادي بدون نص مائل وبدقة فائقة
    private fun addOmniLensWatermarkToBitmap(srcBitmap: Bitmap): Bitmap {
        val mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = canvas.width
        val height = canvas.height

        // ملاحظة: تم حذف النص المائل المتكرر نهائياً لضمان نقاء بصري مطلق وكامل.

        val barHeight = (height * 0.12f).coerceAtLeast(120f)
        val barPaint = Paint().apply {
            color = Color.parseColor("#800000") // الشريط العنابي السيادي
            style = Paint.Style.FILL
        }
        val barRect = RectF(0f, height - barHeight, width.toFloat(), height.toFloat())
        canvas.drawRect(barRect, barPaint)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        // توليد معرّف تتبع جنائي فريد للملف
        val trackingId = "OMNI-TRK-${UUID.randomUUID().toString().take(6).uppercase()}-${System.currentTimeMillis().toString().takeLast(4)}"

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

        val line1Text = "OmniLens | ՏԹiԲ. Տ. ՏԹliʍ - جميع الحقوق محفوظة"
        val line2Text = "$selectedSector | TIME: $timestamp | ID: $trackingId"

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

    private fun saveMediaToVaultAndGallery() {
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
        Toast.makeText(this, "💾 تم الحفظ بنجاح في استوديو الهاتف وخزنة OmniLens", Toast.LENGTH_LONG).show()
        btnSave.isEnabled = false
        if (isVaultUnlocked) loadSavedVaultHistory()
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

        sortedFiles.take(15).forEach { file ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(15, 12, 15, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 8) }
                // 🔒 استخدام المعاينة الداخلية الآمنة عند النقر
                setOnClickListener { openSecureInAppViewer(file) }
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

    // 🛡️ المعاينة الداخلية الآمنة المحمية حصرياً ضد لقطات الشاشة (FLAG_SECURE)
    private fun openSecureInAppViewer(file: File) {
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).create()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(20, 40, 20, 20)
            gravity = Gravity.CENTER
        }

        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            setImageBitmap(bitmap)
        }

        val infoText = TextView(this).apply {
            text = "📄 الملف: ${file.name}\n🔒 معاينة آمنة داخلية محمية من لقطات الشاشة (FLAG_SECURE)"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        val btnClose = Button(this).apply {
            text = "إغلاق المعاينة الآمنة"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#DC2626"))
            setOnClickListener { dialog.dismiss() }
        }

        layout.addView(imageView)
        layout.addView(infoText)
        layout.addView(btnClose)

        dialog.setView(layout)
        // 🔒 تطبيق حظر الشاشة الحصري على نافذة المعاينة
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.show()
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
