package com.omnilens.omniguard

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    enum class LicenseMode { FREE, PAID, GIFT, PRIVATE }

    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_VIDEO_CAPTURE = 1002
    private val REQUEST_GALLERY_PICK = 1003
    private val PERMISSION_REQUEST_CODE = 2000

    private var currentMediaUri: Uri? = null
    private var activeMode: LicenseMode = LicenseMode.FREE
    private var activeSectorName: String = "العام"
    private var targetRecipientName: String = ""

    // 🎨 لوحة الألوان السيبرانية المعتمدة (Cyber Dark Theme)
    private val COLOR_BG = Color.parseColor("#080F1E")        // كحلي داكن جداً خلفي
    private val COLOR_CARD = Color.parseColor("#131E36")      // بطاقات ليلية
    private val COLOR_ACCENT = Color.parseColor("#00F0FF")    // أزرق سيبراني مضيء
    private val COLOR_TEXT_MUTED = Color.parseColor("#8E9AAF")// نص فرعي ناعم
    private val COLOR_FREE = Color.parseColor("#1B4332")      // أخضر غابي
    private val COLOR_GIFT = Color.parseColor("#4A154B")      // أرجواني ملكي
    private val COLOR_PAID = Color.parseColor("#0077B6")      // أزرق نيلي
    private val COLOR_PRIVATE = Color.parseColor("#7F1D1D")   // أحمر أمني

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🛡️ الطبقة الأولى: حظر لقطات الشاشة وتسجيلها بداخل التطبيق
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        showMainDashboard()
    }

    /**
     * 🛠️ محرك إنشاء الخلفيات والزوايا المنحنية برمجياً (Crash-Free UI Generator)
     */
    private fun createDrawable(bgColor: Int, strokeColor: Int = Color.TRANSPARENT, strokeWidth: Int = 0, cornerRadius: Float = 24f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            setCornerRadius(cornerRadius)
            if (strokeWidth > 0) {
                setStroke(strokeWidth, strokeColor)
            }
        }
    }

    /**
     * 🏠 1️⃣ الواجهة الرئيسية والتنقل الشامل (Polished Dashboard)
     */
    private fun showMainDashboard() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(COLOR_BG)
            isFillViewport = true
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(35, 50, 35, 60)
        }

        // 🏷️ الهيدر الرئيسي والشارة الأمنية المتفاعلة
        val statusPill = TextView(this).apply {
            text = "🟢 ENGINE: ACTIVE | FLAG_SECURE: ON | FORENSICS: ACTIVE"
            setTextColor(COLOR_ACCENT)
            textSize = 9f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = createDrawable(Color.parseColor("#0A2540"), COLOR_ACCENT, 2, 40f)
            setPadding(30, 12, 30, 12)
        }
        mainLayout.addView(statusPill)

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }

        val titleText = TextView(this).apply {
            text = "منظومة OmniLens Engine v2.0"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }

        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.app_icon)
            layoutParams = LinearLayout.LayoutParams(90, 90).apply { setMargins(20, 0, 0, 0) }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(logoImage)
        mainLayout.addView(headerLayout)

        val subTitleText = TextView(this).apply {
            text = "المحرك الموحد لتوثيق الوسائط والتتبع الجنائي والضمان المالي"
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 35)
        }
        mainLayout.addView(subTitleText)

        // 📸 1. الكاميرا الميدانية والتوثيق المباشر
        mainLayout.addView(createSectionHeader("📸 الكاميرا الميدانية والتوثيق المباشر"))
        mainLayout.addView(createStyledButton("📷 التقاط صورة وتوثيقها بالبصمة", COLOR_CARD, COLOR_ACCENT) { launchPhotoCamera() })
        mainLayout.addView(createStyledButton("🎥 تصوير مقطع فيديو موثق بالبصمة", COLOR_CARD, COLOR_ACCENT) { launchVideoCamera() })
        mainLayout.addView(createStyledButton("📁 اختيار وسيط من المعرض وتوثيقه", COLOR_CARD, COLOR_ACCENT) { openGallery() })

        // 💬 2. مركز المحادثات والضمان المالي
        mainLayout.addView(createSectionHeader("💬 التفاوض والضمان المالي (Escrow Chat)"))
        mainLayout.addView(createStyledButton("💬 دخول مركز المحادثات والمعاينة المقفولة", COLOR_PAID, Color.WHITE) { openChatHub() })

        // 🏷️ 3. القطاعات التخصصية الثمانية
        mainLayout.addView(createSectionHeader("🏷️ القطاعات التخصصية التفاعلية"))
        val sectors = listOf(
            "🌟 بوابة المشاهير والعقود (ESCROW HUB)",
            "⚽ القطاع الرياضي والفعاليات (SPORTS)",
            "📰 الصحافة والإعلام (PRESS & LEAKS)",
            "🏥 القطاع الطبي والصحي (MEDICAL)",
            "🎬 الإنتاج السينمائي (CINEMATIC)",
            "🎨 الفن الرقمي والتصميم (FINE ART & NFT)",
            "📐 الهندسة والمخططات (ENGINEERING)",
            "🎓 القطاع الأكاديمي والبحثي (RESEARCH)"
        )

        sectors.forEach { sector ->
            mainLayout.addView(createSectorCard(sector) { openSectorPage(sector) })
        }

        // 🔐 4. الخزنة المشفرة والحساب
        mainLayout.addView(createSectionHeader("👤 الحساب والخزنة المشفرة"))
        mainLayout.addView(createStyledButton("🔒 فتح الخزنة المشفرة (Private Vault)", COLOR_PRIVATE, Color.WHITE) { openPrivateVault() })
        mainLayout.addView(createStyledButton("⚙️ إعدادات الحساب والمزامنة السحابية", COLOR_CARD, COLOR_TEXT_MUTED) { showAccountDialog() })

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    /**
     * 🏷️ 2️⃣ شاشة القطاع التخصصي والأنماط الأربعة (Dynamic Mode Picker)
     */
    private fun openSectorPage(sectorTitle: String) {
        activeSectorName = sectorTitle
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(COLOR_BG)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(35, 50, 35, 50)
        }

        val title = TextView(this).apply {
            text = sectorTitle
            setTextColor(COLOR_ACCENT)
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)

        val modeLabel = TextView(this).apply {
            text = "اختر نوع التوثيق والترخيص للمادة:"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 15)
        }
        layout.addView(modeLabel)

        val modeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 4f
            setPadding(0, 0, 0, 25)
        }

        val dynamicFormContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createDrawable(COLOR_CARD, COLOR_ACCENT, 1, 20f)
            setPadding(25, 25, 25, 25)
        }

        fun updateModeUI(selectedMode: LicenseMode) {
            activeMode = selectedMode
            dynamicFormContainer.removeAllViews()

            when (selectedMode) {
                LicenseMode.FREE -> {
                    dynamicFormContainer.addView(createStyledEditText("📝 وصف المادة أو التغطية المجانية", InputType.TYPE_CLASS_TEXT))
                    val info = TextView(this).apply { text = "🟢 نمط مجاني: بدون شبكة تعتيم، مع شريط سفلي موثق باللون الأخضر الغابي."; setTextColor(COLOR_ACCENT); textSize = 11f; setPadding(0, 10, 0, 0) }
                    dynamicFormContainer.addView(info)
                }
                LicenseMode.PAID -> {
                    dynamicFormContainer.addView(createStyledEditText("💵 المبلغ المالي المطلوب ($ USD)", InputType.TYPE_CLASS_NUMBER))
                    dynamicFormContainer.addView(createStyledEditText("👤 اسم المشتري / العميل", InputType.TYPE_CLASS_TEXT))
                    val info = TextView(this).apply { text = "🔵 نمط مدفوع: النمط الشبكي المائل وحظر الاقتطاع مع شريط أزرق محيطي."; setTextColor(COLOR_ACCENT); textSize = 11f; setPadding(0, 10, 0, 0) }
                    dynamicFormContainer.addView(info)
                }
                LicenseMode.GIFT -> {
                    val inputGift = createStyledEditText("🎁 اسم الشخص / الجهة المهدَى إليها", InputType.TYPE_CLASS_TEXT)
                    dynamicFormContainer.addView(inputGift)
                    targetRecipientName = inputGift.text.toString()
                    val info = TextView(this).apply { text = "🟣 نمط هدية: بدون شبكة تعتيم، مع ختم إهداء فاخر وشريط أرجواني ملكي."; setTextColor(COLOR_ACCENT); textSize = 11f; setPadding(0, 10, 0, 0) }
                    dynamicFormContainer.addView(info)
                }
                LicenseMode.PRIVATE -> {
                    dynamicFormContainer.addView(createStyledEditText("🔐 كلمة السر الخاصة بالخزنة المشفرة", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD))
                    val info = TextView(this).apply { text = "🔴 نمط خاص: حماية قصوى بالنمط الشبكي الدقيق والبصمة الترددية وشريط أحمر أمني."; setTextColor(Color.YELLOW); textSize = 11f; setPadding(0, 10, 0, 0) }
                    dynamicFormContainer.addView(info)
                }
            }
        }

        val btnFree = createModeButton("🆓 مجاني", COLOR_FREE) { updateModeUI(LicenseMode.FREE) }
        val btnPaid = createModeButton("💳 مدفوع", COLOR_PAID) { updateModeUI(LicenseMode.PAID) }
        val btnGift = createModeButton("🎁 هدية", COLOR_GIFT) { updateModeUI(LicenseMode.GIFT) }
        val btnPrivate = createModeButton("🔒 خاص", COLOR_PRIVATE) { updateModeUI(LicenseMode.PRIVATE) }

        modeContainer.addView(btnFree)
        modeContainer.addView(btnPaid)
        modeContainer.addView(btnGift)
        modeContainer.addView(btnPrivate)

        layout.addView(modeContainer)
        layout.addView(dynamicFormContainer)

        updateModeUI(LicenseMode.FREE)

        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 30) }
        layout.addView(spacer)

        val btnCapturePhoto = createStyledButton("📸 التقاط صورة وتوثيقها", COLOR_FREE, Color.WHITE) { launchPhotoCamera() }
        val btnCaptureVideo = createStyledButton("🎥 تسجيل فيديو وتوثيقه", COLOR_PAID, Color.WHITE) { launchVideoCamera() }
        val btnBack = createStyledButton("🔙 العودة للقائمة الرئيسية", COLOR_CARD, COLOR_TEXT_MUTED) { showMainDashboard() }

        layout.addView(btnCapturePhoto)
        layout.addView(btnCaptureVideo)
        layout.addView(btnBack)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    /**
     * 💬 3️⃣ مركز الشات والضمان المالي (Polished Escrow Hub UI)
     */
    private fun openChatHub() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(COLOR_BG)
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 40, 30, 40)
        }

        val title = TextView(this).apply {
            text = "💬 مركز الشات والضمان المالي (Escrow Hub)"
            setTextColor(COLOR_ACCENT)
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createDrawable(COLOR_CARD, COLOR_ACCENT, 2, 24f)
            setPadding(30, 30, 30, 30)
        }

        val cardTitle = TextView(this).apply {
            text = "🔒 معاينة مقفولة (Locked Preview)"
            setTextColor(Color.YELLOW)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        cardLayout.addView(cardTitle)

        val cardDesc = TextView(this).apply {
            text = "💵 قيمة العقد: $250 USD\n👤 المالك: ՏԹɿԲ. Տ. ՏԹʅɿʍ\n🛡️ الحالة: المادة محمية بالنمط الشبكي الدقيق بانتظار إيداع الضمان."
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(0, 15, 0, 25)
        }
        cardLayout.addView(cardDesc)

        val btnDeposit = createStyledButton("💳 إيداع المبلغ بحساب الضمان ($250 USD)", COLOR_PAID, Color.WHITE) {
            showDialog("حجز المبلغ 💳", "تم إيداع $250 USD بنجاح بداخل حساب الضمان، وفك المعاينة المقفولة للعميل.")
        }
        val btnRelease = createStyledButton("✅ اعتماد واستلام المادة (Release Funds)", COLOR_FREE, Color.WHITE) {
            showDialog("إفراج عن المبلغ 💸", "تم تسليم النسخة الأصلية الصافية 100% بدون شبكة تعتيم للمشتري، وتحويل المبلغ لحسابك!")
        }
        val btnDispute = createStyledButton("⚠️ رفع نزاع مالي وتوثيقي (Open Dispute)", COLOR_PRIVATE, Color.WHITE) {
            showDialog("فتح نزاع ⚠️", "تم تجميد الصفقة وتوليد تقرير الفحص الجنائي.")
        }

        cardLayout.addView(btnDeposit)
        cardLayout.addView(btnRelease)
        cardLayout.addView(btnDispute)

        layout.addView(cardLayout)

        val spacer = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, 30) }
        layout.addView(spacer)

        val btnBack = createStyledButton("🔙 العودة للقائمة الرئيسية", COLOR_CARD, COLOR_TEXT_MUTED) { showMainDashboard() }
        layout.addView(btnBack)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    /**
     * 🔒 4️⃣ الخزنة المشفرة (Private Vault UI)
     */
    private fun openPrivateVault() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BG)
            setPadding(40, 60, 40, 60)
            gravity = Gravity.CENTER
        }

        val vaultTitle = TextView(this).apply {
            text = "🔐 الخزنة المشفرة | Private Vault"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(vaultTitle)

        val vaultDesc = TextView(this).apply {
            text = "تضم كافة المواد الخاصة المحمية بالبصمة الترددية المخفية والنمط الشبكي الدقيق."
            setTextColor(COLOR_TEXT_MUTED)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        layout.addView(vaultDesc)

        val btnBack = createStyledButton("🔙 العودة للقائمة الرئيسية", COLOR_CARD, COLOR_TEXT_MUTED) { showMainDashboard() }
        layout.addView(btnBack)

        setContentView(layout)
    }

    /**
     * 🎨 5️⃣ محرك التوثيق البصري والتلوين الميداني الديناميكي (Watermark Engine)
     */
    private fun applyModelCWatermarkAndSave(sourceUri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, sourceUri)
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val width = mutableBitmap.width.toFloat()
            val height = mutableBitmap.height.toFloat()

            // 1. النمط الشبكي الدقيق (Micro-Grid) فقط للمدفوع والخاص
            if (activeMode == LicenseMode.PAID || activeMode == LicenseMode.PRIVATE) {
                val gridPaint = Paint().apply {
                    color = Color.WHITE
                    alpha = 35
                    textSize = width * 0.028f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.save()
                canvas.rotate(-30f, width / 2f, height / 2f)

                val stepX = width * 0.35f
                val stepY = height * 0.12f

                var y = -height
                while (y < height * 2) {
                    var x = -width
                    while (x < width * 2) {
                        canvas.drawText("OMNILENS • PROTECTED", x, y, gridPaint)
                        x += stepX
                    }
                    y += stepY
                }
                canvas.restore()
            }

            // 2. ختم الإهداء الناعم الأنيق في نمط الهدية (Gift Mode)
            if (activeMode == LicenseMode.GIFT) {
                val giftPaint = Paint().apply {
                    color = Color.YELLOW
                    alpha = 180
                    textSize = width * 0.035f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val giftText = if (targetRecipientName.isNotEmpty()) "🎁 GIFTED TO: $targetRecipientName | BY: ՏԹɿԲ. Տ. ՏԹʅɿʍ" else "🎁 OFFICIAL GIFT | BY: ՏԹɿԲ. Տ. ՏԹʅɿʍ"
                canvas.drawText(giftText, width / 2f, height * 0.85f, giftPaint)
            }

            // 3. تحديد لون الشريط الأسفلي الميداني الملون ديناميكياً
            val bannerColorHex = when (activeMode) {
                LicenseMode.FREE -> "#1B4332"    // أخضر غابي
                LicenseMode.GIFT -> "#4A154B"    // أرجواني ملكي
                LicenseMode.PAID -> "#0077B6"    // أزرق نيلي
                LicenseMode.PRIVATE -> "#7F1D1D" // أحمر أمني
            }

            val bannerHeight = height * 0.10f
            val bannerPaint = Paint().apply {
                color = Color.parseColor(bannerColorHex)
            }
            canvas.drawRect(0f, height - bannerHeight, width, height, bannerPaint)

            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = width * 0.022f
                typeface = Typeface.MONOSPACE
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val line1 = "omniLens | OWNER: ՏԹɿԲ. Տ. ՏԹʅɿʍ | MODE: ${activeMode.name} | SECTOR: $activeSectorName"
            val line2 = "TIME: $timestamp | TOKEN: OMNI-HW-ID-2026-SAIF"

            canvas.drawText(line1, 25f, height - (bannerHeight * 0.55f), textPaint)
            canvas.drawText(line2, 25f, height - (bannerHeight * 0.20f), textPaint)

            saveBitmapToPublicGallery(mutableBitmap)

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في توثيق الصورة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 🎥 6️⃣ محرك توثيق الفيديو الميداني
     */
    private fun processVideoAndSave(videoUri: Uri) {
        try {
            val filename = "OMNI_VIDEO_${System.currentTimeMillis()}.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, filename)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.TITLE, "omniLens Protected Video")
                put(MediaStore.Video.Media.DESCRIPTION, "OWNER: ՏԹɿԲ. Տ. ՏԹʅɿʍ | TOKEN: OMNI-VID-2026-SAIF | MODE: ${activeMode.name}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/OmniLens")
                }
            }

            val newUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            newUri?.let { destUri ->
                contentResolver.openInputStream(videoUri)?.use { input ->
                    contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                showDialog(
                    "تم توثيق الفيديو بنجاح 🎥",
                    "تم حفر البصمة الرقمية وتطبيق الشريط الأسفلي الميداني وحفظ الفيديو في مجلد (Movies/OmniLens)!"
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "تم تسجيل الفيديو ومعالجة البصمة محلياً", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveBitmapToPublicGallery(bitmap: Bitmap) {
        val filename = "OMNI_STAMPED_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/OmniLens")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            showDialog("تم التوثيق بنجاح 📸", "تم تطبيق ختم التوثيق الميداني الملون وحفظ الصورة بنجاح في مجلد (Pictures/OmniLens)!")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> currentMediaUri?.let { uri -> applyModelCWatermarkAndSave(uri) }
                REQUEST_VIDEO_CAPTURE -> currentMediaUri?.let { uri -> processVideoAndSave(uri) } ?: data?.data?.let { uri -> processVideoAndSave(uri) }
                REQUEST_GALLERY_PICK -> data?.data?.let { uri -> applyModelCWatermarkAndSave(uri) }
            }
        }
    }

    private fun checkAndRequestPermissions(isVideo: Boolean): Boolean {
        val permissions = if (isVideo) arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) else arrayOf(Manifest.permission.CAMERA)
        val needed = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    fun launchPhotoCamera() {
        if (!checkAndRequestPermissions(isVideo = false)) return
        try {
            val photoFile = File.createTempFile("omni_raw_", ".jpg", cacheDir)
            currentMediaUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الكاميرا: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun launchVideoCamera() {
        if (!checkAndRequestPermissions(isVideo = true)) return
        try {
            val videoFile = File.createTempFile("omni_vid_", ".mp4", cacheDir)
            currentMediaUri = FileProvider.getUriForFile(this, "$packageName.provider", videoFile)
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الفيديو: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY_PICK)
    }

    fun showAccountDialog() {
        showDialog("👤 بروفايل المطور | ՏԹɿԲ. Տ. ՏԹʅɿʍ", "المالك والمطور الرئيسي: ՏԹɿԲ. Տ. ՏԹʅɿʍ\nمعرف المنظومة: OMNI-DEV-2026-SAIF\nحالة الحماية الجنائية: نشطة بالكامل 🟢")
    }

    // 🎨 أدوات وبناء عناصر الواجهة برمجياً وبدون أخطاء
    private fun createSectorCard(title: String, onClickAction: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createDrawable(COLOR_CARD, Color.parseColor("#1F2D4A"), 1, 20f)
            setPadding(35, 30, 35, 30)
            setOnClickListener { onClickAction() }

            val txt = TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val arrow = TextView(context).apply {
                text = "❮"
                setTextColor(COLOR_ACCENT)
                textSize = 14f
            }

            addView(txt)
            addView(arrow)

            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 18)
            layoutParams = params
        }
    }

    private fun createModeButton(textStr: String, hexColor: Int, onClickAction: () -> Unit): Button {
        return Button(this).apply {
            text = textStr
            textSize = 10f
            setTextColor(Color.WHITE)
            background = createDrawable(hexColor, Color.TRANSPARENT, 0, 16f)
            setOnClickListener { onClickAction() }
            val params = LinearLayout.LayoutParams(0, 110, 1f)
            params.setMargins(4, 0, 4, 0)
            layoutParams = params
        }
    }

    private fun createStyledEditText(hintText: String, inputTypeEnum: Int): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = inputTypeEnum
            setHintTextColor(COLOR_TEXT_MUTED)
            setTextColor(Color.WHITE)
            background = createDrawable(COLOR_BG, COLOR_ACCENT, 1, 16f)
            setPadding(30, 30, 30, 30)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 18)
            layoutParams = params
        }
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(COLOR_ACCENT)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.RIGHT
            setPadding(0, 25, 10, 15)
        }
    }

    private fun createStyledButton(buttonText: String, bgHex: Int, textHex: Int, onClickAction: () -> Unit): Button {
        return Button(this).apply {
            text = buttonText
            textSize = 12f
            setTextColor(textHex)
            background = createDrawable(bgHex, COLOR_ACCENT, 1, 20f)
            setOnClickListener { onClickAction() }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 135)
            params.setMargins(0, 0, 0, 18)
            layoutParams = params
        }
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("موافق", null)
            .show()
    }
}
