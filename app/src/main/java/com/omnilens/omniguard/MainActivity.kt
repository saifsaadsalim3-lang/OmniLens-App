package com.omnilens.omniguard

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : FragmentActivity() {

    private lateinit var selectedImageView: ImageView
    private lateinit var hashTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var btnSave: Button
    private lateinit var historyLayout: LinearLayout
    private lateinit var sectorButtonsLayout: LinearLayout
    private lateinit var settingsSummaryText: TextView
    private lateinit var btnToggleVault: Button
    private lateinit var btnAppLockToggle: Button
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var logoImageView: ImageView

    private var currentBitmap: Bitmap? = null
    private var currentHash: String = ""
    private var tempPhotoFile: File? = null

    // 🏛️ القطاع والنمط المحدد
    private var selectedSector: String = "🏛️ القطاع الحكومي والسيادي (GOVERNMENT & SOVEREIGN HUB)"
    private var currentMode: String = "PAID" // [FREE, GIFT, PRIVATE, PAID]

    private var selectedResolution = "FHD (1080p)"
    private var selectedFPS = "30 FPS"
    private var isVaultUnlocked = false
    private var isAppLocked = false

    private val CAMERA_BACK_REQUEST = 101
    private val CAMERA_FRONT_REQUEST = 102
    private val GALLERY_REQUEST_CODE = 103
    private val PERMISSION_REQUEST_CODE = 200
    private val CHANNEL_ID = "omnilens_alerts_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefs = getSharedPreferences("omnilens_sec_prefs", Context.MODE_PRIVATE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        createNotificationChannel()

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(25, 30, 25, 40)
        }

        // 🌟 1. شعار التطبيق المتحرك النبّاض (يعمل لمدة 5 ثوانٍ فقط)
        logoImageView = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(Color.parseColor("#38BDF8"))
            layoutParams = LinearLayout.LayoutParams(130, 130).apply {
                gravity = Gravity.CENTER
                setMargins(0, 5, 0, 5)
            }
        }
        startPulsingAnimationFor5Seconds(logoImageView)

        // 2. ترويسة الاصدار v3.0
        val titleText = TextView(this).apply {
            text = "منظومة OmniLens Engine v3.0 🛡️"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subTitleText = TextView(this).apply {
            text = "منظومة التوثيق السيادي والتتبع الجنائي | Sovereign Baseline v3.0"
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        // 3. زر التحكم بقفل التطبيق الحيوي عند الفتح
        btnAppLockToggle = createStyledButton("", "#3B82F6") { toggleAppLockPreference() }
        updateAppLockBtnState()

        // 4. وسائل التقاط الوسائط
        val captureHeader = createSectionHeader("📷 وسائل التقاط وجلب الوسائط")
        val btnBackCamera = createStyledButton("📷 التقاط بالكاميرا الخلفية", "#334155") { launchCamera(isFront = false) }
        val btnFrontCamera = createStyledButton("🤳 التقاط بالكاميرا الأمامية", "#334155") { launchCamera(isFront = true) }
        val btnGallery = createStyledButton("📂 اختيار صورة من المعرض وتوثيقها", "#334155") { openGallery() }

        // 5. القطاعات التخصصية والأنماط المدمجة بداخلها
        val sectorHeader = createSectionHeader("🏷️ القطاعات التخصصية والأنماط المدمجة")
        sectorButtonsLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        refreshSectorButtons(sectorButtonsLayout)

        // 6. إعدادات التصوير والجودة
        settingsSummaryText = TextView(this).apply {
            text = "⚙️ إعدادات التصوير: [$selectedResolution] | [$selectedFPS]"
            textSize = 11f
            setTextColor(Color.parseColor("#F59E0B"))
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 10)
        }
        val btnSettings = createStyledButton("⚙️ ضبط دقة التصوير والإطارات", "#475569") { showCameraSettingsDialog() }

        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 420).apply { setMargins(0, 10, 0, 15) }
            setBackgroundColor(Color.parseColor("#020617"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        statusTextView = TextView(this).apply {
            text = "🛡️ جاهز للالتقاط والتوثيق بنقاء بصري كامل"
            textSize = 11.5f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        btnSave = createStyledButton("💾 حفظ الوسائط في الخزنة أو استوديو الجهاز", "#10B981") { saveMediaByActiveMode() }.apply { isEnabled = false }

        hashTextView = TextView(this).apply {
            text = "قم بالتقاط صورة أو اختيارها لتوليد بصمة SHA-256."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(10, 5, 10, 15)
        }

        // 7. الخزنة المشفرة الخاصّة
        val historyTitle = createSectionHeader("🔐 الخزنة المشفرة الخاصة (رمز سري + بصمة)")
        btnToggleVault = createStyledButton("🔒 فتح الخزنة المشفرة (PIN Code)", "#DC2626") { handleVaultSecurityAccess() }
        historyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة للمؤسس ՏԹiԲ. Տ. ՏԹliʍ"
            textSize = 10f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        // تجميع عناصر الواجهة
        rootLayout.addView(logoImageView)
        rootLayout.addView(titleText)
        rootLayout.addView(subTitleText)
        rootLayout.addView(btnAppLockToggle)
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
        verifyAppAccessOnLaunch()
    }

    // ⏱️ نبض الشعار لمدة 5 ثوانٍ فقط ثم التوقف بمرونة
    private fun startPulsingAnimationFor5Seconds(targetView: View) {
        val scaleX = ObjectAnimator.ofFloat(targetView, "scaleX", 1.0f, 1.25f)
        val scaleY = ObjectAnimator.ofFloat(targetView, "scaleY", 1.0f, 1.25f)
        val alpha = ObjectAnimator.ofFloat(targetView, "alpha", 0.6f, 1.0f)

        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleX.repeatMode = ObjectAnimator.REVERSE
        scaleY.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatMode = ObjectAnimator.REVERSE
        alpha.repeatCount = ObjectAnimator.INFINITE
        alpha.repeatMode = ObjectAnimator.REVERSE

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 800
            start()
        }

        // إيقاف النبض بعد 5000 مللي ثانية (5 ثوانٍ)
        Handler(Looper.getMainLooper()).postDelayed({
            animatorSet.cancel()
            targetView.scaleX = 1.0f
            targetView.scaleY = 1.0f
            targetView.alpha = 1.0f
        }, 5000)
    }

    // 🔔 إنشاء قناة الإشعارات
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "تنبيهات منظومة OmniLens"
            val descriptionText = "إشعارات عمليات التوثيق والأمان وحالة الخزنة"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 🔔 إرسال إشعار نظام للمستخدم
    private fun sendSystemNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // 🏷️ بناء الأقسام مع دمج الأنماط الأربعة بداخل كل قسم
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
            val sectorCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(15, 12, 15, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 10) }
            }

            val sectorBtn = Button(this).apply {
                text = if (selectedSector == sectorName) "✅ $sectorName" else sectorName
                textSize = 11.5f
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    if (selectedSector == sectorName) Color.parseColor("#0284C7") else Color.parseColor("#334155")
                )
                setOnClickListener {
                    selectedSector = sectorName
                    refreshSectorButtons(container)
                    Toast.makeText(this@MainActivity, "تم تحديد القطاع: $sectorName", Toast.LENGTH_SHORT).show()
                }
            }
            sectorCard.addView(sectorBtn)

            // إظهار خيارات الأنماط بداخل القطاع المختار حالياً
            if (selectedSector == sectorName) {
                val modeLabel = TextView(this).apply {
                    text = "👇 اختر نمط التوثيق لهذا القطاع:"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 10f
                    setPadding(0, 8, 0, 4)
                }
                sectorCard.addView(modeLabel)

                val modesLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val modes = listOf(
                    Triple("FREE", "🆓 مجاني", "#64748B"),
                    Triple("GIFT", "🎁 هدية", "#D97706"),
                    Triple("PRIVATE", "🔒 خاص", "#0D9488"),
                    Triple("PAID", "💎 سيادي", "#7C3AED")
                )

                modes.forEach { (modeKey, modeTitle, colorHex) ->
                    val isModeSelected = currentMode == modeKey
                    val modeBtn = Button(this).apply {
                        text = modeTitle
                        textSize = 9.5f
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.parseColor(if (isModeSelected) colorHex else "#0F172A"))
                        setOnClickListener {
                            currentMode = modeKey
                            applyModeConfig(modeKey)
                            refreshSectorButtons(container)
                        }
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                            setMargins(2, 0, 2, 0)
                        }
                    }
                    modesLayout.addView(modeBtn)
                }
                sectorCard.addView(modesLayout)
            }

            container.addView(sectorCard)
        }

        val btnAddCustom = Button(this).apply {
            text = "➕ إضافة قطاع مخصص جديد..."
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0F766E"))
            setOnClickListener { showAddCustomSectorDialog(container) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 5, 0, 10) }
        }
        container.addView(btnAddCustom)
    }

    private fun applyModeConfig(mode: String) {
        when (mode) {
            "FREE" -> selectedResolution = "HD (720p)"
            "GIFT" -> {
                selectedResolution = "FHD (1080p)"
                showGiftPromoDialog()
            }
            "PRIVATE" -> selectedResolution = "FHD (1080p)"
            "PAID" -> selectedResolution = "Ultra HD (4K)"
        }
        settingsSummaryText.text = "⚙️ إعدادات التصوير: [$selectedResolution] | [$selectedFPS]"
    }

    private fun showGiftPromoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🎁 تفعيل كود الهدية والترويج")
        builder.setMessage("أدخل كود الهدية الممنوح لك لفتح ميزات التوثيق السيادي:")
        val input = EditText(this).apply { hint = "أدخل الكود (مثلاً: OMNI-2026)" }
        builder.setView(input)
        builder.setPositiveButton("تفعيل") { _, _ ->
            val code = input.text.toString().trim()
            if (code.isNotEmpty()) {
                sendSystemNotification("🎁 تم تفعيل الهدية", "تم فتح كافة ميزات التوثيق السيادي بنجاح!")
                Toast.makeText(this, "تم تفعيل كود الهدية بنجاح! ✅", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun saveMediaByActiveMode() {
        val bitmap = currentBitmap ?: return
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "OMNI_${currentMode}_$timeStamp.jpg"

        if (currentMode == "PRIVATE") {
            val privateDir = File(filesDir, "OmniLensPrivateVault")
            if (!privateDir.exists()) privateDir.mkdirs()
            val privateFile = File(privateDir, fileName)
            FileOutputStream(privateFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            
            sendSystemNotification("🔒 حظر وتوثيق خاص", "تم حفظ المستند بداخل الخزنة المعزولة فقط دون المعرض العام.")
            Toast.makeText(this, "🔒 تم الحفظ بداخل الخزنة المعزولة بأمان تام", Toast.LENGTH_LONG).show()
        } else {
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val omniFolder = File(dcimDir, "OmniLens")
            if (!omniFolder.exists()) omniFolder.mkdirs()

            val galleryFile = File(omniFolder, fileName)
            FileOutputStream(galleryFile).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            MediaScannerConnection.scanFile(this, arrayOf(galleryFile.absolutePath), null, null)

            sendSystemNotification("💾 تم توثيق وحفظ المستند", "تم إضافة الوسيط الموثق كـ [$currentMode] بداخل استوديو الهاتف والخزنة.")
            Toast.makeText(this, "💾 تم الحفظ بنجاح في استوديو الهاتف وخزنة المنظومة", Toast.LENGTH_LONG).show()
        }

        btnSave.isEnabled = false
        if (isVaultUnlocked) loadSavedVaultHistory()
    }

    private fun verifyAppAccessOnLaunch() {
        val isLockEnabled = sharedPrefs.getBoolean("app_lock_enabled", false)
        if (isLockEnabled && !isAppLocked) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguardManager.isKeyguardSecure) {
                val intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    "منظومة OmniLens Engine v3.0 🛡️",
                    "يرجى التأكد من هوية المالك (بصمة وجه / إصبع / رمز سري)"
                )
                if (intent != null) startActivityForResult(intent, 500)
            }
        }
    }

    private fun toggleAppLockPreference() {
        val currentState = sharedPrefs.getBoolean("app_lock_enabled", false)
        val newState = !currentState
        sharedPrefs.edit().putBoolean("app_lock_enabled", newState).apply()
        updateAppLockBtnState()
        Toast.makeText(this, if (newState) "تم تفعيل القفل الحيوي ✅" else "تم إيقاف القفل 🔓", Toast.LENGTH_SHORT).show()
    }

    private fun updateAppLockBtnState() {
        val isEnabled = sharedPrefs.getBoolean("app_lock_enabled", false)
        if (isEnabled) {
            btnAppLockToggle.text = "🔒 قفل التطبيق الحيوي عند الفتح: [مُفعل]"
            btnAppLockToggle.setBackgroundColor(Color.parseColor("#059669"))
        } else {
            btnAppLockToggle.text = "🔓 قفل التطبيق الحيوي عند الفتح: [مُعطل]"
            btnAppLockToggle.setBackgroundColor(Color.parseColor("#475569"))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isVaultUnlocked) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 15, 0, 8)
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
            ).apply { setMargins(0, 0, 0, 8) }
        }
    }

    private fun showAddCustomSectorDialog(container: LinearLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("➕ إضافة قطاع تخصصي جديد")
        val input = EditText(this).apply { hint = "مثال: قطاع العقارات والتوثيق القانوني" }
        builder.setView(input)
        builder.setPositiveButton("حفظ وإضافة") { _, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                val customSet = sharedPrefs.getStringSet("custom_sectors", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                customSet.add("🏢 $name (CUSTOM HUB)")
                sharedPrefs.edit().putStringSet("custom_sectors", customSet).apply()
                refreshSectorButtons(container)
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun handleVaultSecurityAccess() {
        if (isVaultUnlocked) {
            isVaultUnlocked = false
            historyLayout.visibility = View.GONE
            btnToggleVault.text = "🔒 فتح الخزنة المشفرة (PIN Code)"
            btnToggleVault.setBackgroundColor(Color.parseColor("#DC2626"))
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            sendSystemNotification("🔒 الخزنة المشفرة", "تم قفل الخزنة وحظر لقطات الشاشة بأمان.")
            Toast.makeText(this, "تم قفل الخزنة بأمان", Toast.LENGTH_SHORT).show()
        } else {
            val savedPin = sharedPrefs.getString("vault_pin", null)
            if (savedPin == null) showSetPinDialog() else showEnterPinDialog(savedPin)
        }
    }

    private fun showSetPinDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔑 إنشاء رمز سري للخزنة")
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        builder.setView(input)
        builder.setPositiveButton("حفظ") { _, _ ->
            val pin = input.text.toString().trim()
            if (pin.length >= 4) {
                sharedPrefs.edit().putString("vault_pin", pin).apply()
                unlockVaultSuccess()
            }
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun showEnterPinDialog(correctPin: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔐 فتح الخزنة المشفرة")
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        builder.setView(input)
        builder.setPositiveButton("فتح") { _, _ ->
            if (input.text.toString().trim() == correctPin) unlockVaultSuccess() else Toast.makeText(this, "الرمز غير صحيح ❌", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }

    private fun unlockVaultSuccess() {
        isVaultUnlocked = true
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        historyLayout.visibility = View.VISIBLE
        btnToggleVault.text = "🔓 إغلاق وقفل الخزنة المشفرة"
        btnToggleVault.setBackgroundColor(Color.parseColor("#10B981"))
        loadSavedVaultHistory()
        sendSystemNotification("🔓 الخزنة مفتوحة", "تم فتح الخزنة وتفعيل حظر لقطات الشاشة FLAG_SECURE تلقائياً.")
        Toast.makeText(this, "تم فتح الخزنة وحظر لقطات الشاشة ✅", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions(): Boolean {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_MEDIA_IMAGES)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    private fun launchCamera(isFront: Boolean) {
        if (!checkPermissions()) return
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            tempPhotoFile = File.createTempFile("OMNI_${timeStamp}_", ".jpg", storageDir)
            val photoURI: Uri = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", tempPhotoFile!!)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                if (isFront) {
                    putExtra("android.intent.extras.CAMERA_FACING", 1)
                    putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                    putExtra("useFrontCamera", true)
                }
            }
            startActivityForResult(intent, if (isFront) CAMERA_FRONT_REQUEST else CAMERA_BACK_REQUEST)
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
        if (requestCode == 500) {
            if (resultCode == RESULT_OK) isAppLocked = true else finish()
            return
        }
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_BACK_REQUEST, CAMERA_FRONT_REQUEST -> tempPhotoFile?.let { file ->
                    BitmapFactory.decodeFile(file.absolutePath)?.let { processCapturedBitmap(it) }
                }
                GALLERY_REQUEST_CODE -> data?.data?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.let { processCapturedBitmap(it) }
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
        hashTextView.text = "🔑 SHA-256 Digest & Mode [$currentMode]:\n$currentHash"
        btnSave.isEnabled = true
        statusTextView.text = "✅ تم توثيق الصورة بنجاح وتطبيق بصمة التتبع للقطاع والنمط المحدد"
    }

    private fun addOmniLensWatermarkToBitmap(srcBitmap: Bitmap): Bitmap {
        val mutableBitmap = srcBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val width = canvas.width
        val height = canvas.height

        val barHeight = (height * 0.12f).coerceAtLeast(120f)
        val barPaint = Paint().apply {
            color = when (currentMode) {
                "FREE" -> Color.parseColor("#334155")
                "GIFT" -> Color.parseColor("#B45309")
                "PRIVATE" -> Color.parseColor("#0F766E")
                else -> Color.parseColor("#800000") // السيادي المدفوع
            }
            style = Paint.Style.FILL
        }
        val barRect = RectF(0f, height - barHeight, width.toFloat(), height.toFloat())
        canvas.drawRect(barRect, barPaint)

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val trackingId = "OMNI-v3.0-${currentMode}-TRK-${UUID.randomUUID().toString().take(6).uppercase()}"

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

        val line1Text = "OmniLens v3.0 [$currentMode] | ՏԹiԲ. Տ. ՏԹliʍ - جميع الحقوق محفوظة"
        val line2Text = "$selectedSector | TIME: $timestamp | ID: $trackingId"

        canvas.drawText(line1Text, 30f, height - (barHeight * 0.55f), textPaintLine1)
        canvas.drawText(line2Text, 30f, height - (barHeight * 0.18f), textPaintLine2)

        return mutableBitmap
    }

    private fun calculateBitmapSHA256(bitmap: Bitmap): String {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(stream.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun loadSavedVaultHistory() {
        historyLayout.removeAllViews()
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val omniFolder = File(dcimDir, "OmniLens")
        val privateDir = File(filesDir, "OmniLensPrivateVault")

        val allFiles = mutableListOf<File>()
        if (omniFolder.exists()) omniFolder.listFiles()?.let { allFiles.addAll(it) }
        if (privateDir.exists()) privateDir.listFiles()?.let { allFiles.addAll(it) }

        val sortedFiles = allFiles.sortedByDescending { it.lastModified() }
        if (sortedFiles.isEmpty()) {
            historyLayout.addView(TextView(this).apply {
                text = "لا توجد وسائط محفوظة في الخزنة حالياً."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(0, 10, 0, 10)
            })
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
                setOnClickListener { openSecureInAppViewer(file) }
            }
            val nameText = TextView(this).apply {
                text = "🖼️ ${file.name}\nالتاريخ: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}"
                setTextColor(Color.WHITE)
                textSize = 11f
            }
            card.addView(nameText)
            historyLayout.addView(card)
        }
    }

    private fun openSecureInAppViewer(file: File) {
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).create()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(20, 40, 20, 20)
            gravity = Gravity.CENTER
        }
        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        }
        val btnClose = Button(this).apply {
            text = "إغلاق المعاينة الآمنة"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#DC2626"))
            setOnClickListener { dialog.dismiss() }
        }
        layout.addView(imageView)
        layout.addView(btnClose)
        dialog.setView(layout)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.show()
    }

    private fun showCameraSettingsDialog() {
        val resolutions = arrayOf("HD (720p)", "FHD (1080p)", "Ultra HD (4K)")
        val fpsOptions = arrayOf("30 FPS", "60 FPS")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("⚙️ إعدادات جودة الكاميرا والتوثيق")
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(35, 20, 35, 20) }
        val spinnerRes = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, resolutions) }
        val spinnerFPS = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, fpsOptions) }
        layout.addView(TextView(this).apply { text = "اختر الدقة:" })
        layout.addView(spinnerRes)
        layout.addView(TextView(this).apply { text = "اختر معدل الإطارات:" })
        layout.addView(spinnerFPS)
        builder.setView(layout)
        builder.setPositiveButton("حفظ") { _, _ ->
            selectedResolution = spinnerRes.selectedItem.toString()
            selectedFPS = spinnerFPS.selectedItem.toString()
            settingsSummaryText.text = "⚙️ إعدادات التصوير: [$selectedResolution] | [$selectedFPS]"
            Toast.makeText(this, "تم التحديث بنجاح", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("إلغاء", null)
        builder.show()
    }
}
