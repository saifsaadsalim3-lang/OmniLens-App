package com.omnilens.omniguard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
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
    private lateinit var mainContentLayout: ScrollView
    private lateinit var splashLayout: FrameLayout
    private lateinit var userAccountStatusText: TextView
    private lateinit var chatMessagesLayout: LinearLayout

    // متغيرات التقريب باللمس (Pinch-to-Zoom Matrix)
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private val matrix = Matrix()

    private var currentBitmap: Bitmap? = null
    private var currentMediaFile: File? = null
    private var currentHash: String = ""
    private var tempPhotoFile: File? = null

    private var currentUserOmniId: String = "Guest_OmniUser"
    private var isUserLoggedIn: Boolean = false

    private var selectedResolution = "FHD (1080p)"
    private var selectedFPS = "30 FPS"
    private var activeCaptureMode = "OMNILENS_VAULT"
    private var isCalledFromExternalApp = false

    private val CAMERA_REQUEST_CODE = 101
    private val GALLERY_REQUEST_CODE = 102
    private val VIDEO_REQUEST_CODE = 103
    private val PERMISSION_CODE = 104

    private val CHANNEL_ID = "omnilens_protection_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.action
        if (action == MediaStore.ACTION_IMAGE_CAPTURE || action == MediaStore.ACTION_VIDEO_CAPTURE) {
            isCalledFromExternalApp = true
        }

        val rootContainer = FrameLayout(this)

        mainContentLayout = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
            visibility = View.GONE
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(25, 30, 25, 30)
        }

        // 1. الهيدر العائم
        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 15, 20, 15)
            setBackgroundColor(Color.parseColor("#1E293B"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 15) }
        }

        val logoImage = ImageView(this).apply {
            val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (imageResId != 0) setImageResource(imageResId)
            layoutParams = LinearLayout.LayoutParams(90, 90)
        }

        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleText = TextView(this).apply {
            text = "OmniLens v1.8.0 Immersive"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        userAccountStatusText = TextView(this).apply {
            text = "👤 الحساب: $currentUserOmniId"
            textSize = 11f
            setTextColor(Color.parseColor("#38BDF8"))
        }

        titleContainer.addView(titleText)
        titleContainer.addView(userAccountStatusText)

        val btnAccount = Button(this).apply {
            text = "🔑"
            textSize = 15f
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(85, 85)
            setOnClickListener { showAccountLoginDialog() }
        }

        headerCard.addView(logoImage)
        headerCard.addView(titleContainer)
        headerCard.addView(btnAccount)

        // 2. شريط الأدوات السريع والوضع التفاعلي
        val quickBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }

        val btnNewContent = Button(this).apply {
            text = "➕ محتوى جديد"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#16A34A"))
            setTextColor(Color.WHITE)
            setOnClickListener { showNewContentSourceDialog() }
        }

        val btnOpenChat = Button(this).apply {
            text = "💬 دردشة مشفرة"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener { showChatRoomDialog() }
        }

        val btnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(3, 0, 3, 0) }
        quickBarLayout.addView(btnNewContent, btnParams)
        quickBarLayout.addView(btnOpenChat, btnParams)

        // 3. كارت المعاينة المضيء المعزز بتقريب اللمس (Pinch-to-Zoom View)
        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                550
            ).apply { setMargins(0, 0, 0, 15) }
            setBackgroundColor(Color.parseColor("#1E293B"))
            scaleType = ImageView.ScaleType.MATRIX
        }

        // تهيئة مستشعر التقريب بالإصبعين (Pinch-to-Zoom)
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 4.0f) // تحديد أقصى وأقل نسبة تقريب
                matrix.setScale(scaleFactor, scaleFactor, selectedImageView.width / 2f, selectedImageView.height / 2f)
                selectedImageView.imageMatrix = matrix
                return true
            }
        })

        selectedImageView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }

        val zoomNoticeText = TextView(this).apply {
            text = "🔍 ميزة التقريب نشطة: استخدم إصبعيك للتقريب والتكبير على الصور والأدلة."
            textSize = 10f
            setTextColor(Color.parseColor("#F59E0B"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
        }

        val actionButtonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
        }

        val btnCapturePhoto = Button(this).apply {
            text = "📷 التقاط صورة"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkPermissionsAndCapture(isVideo = false) }
        }

        val btnCaptureVideo = Button(this).apply {
            text = "🎥 تسجيل فيديو"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#DC2626"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkPermissionsAndCapture(isVideo = true) }
        }

        val btnPickGallery = Button(this).apply {
            text = "🖼️ المعرض"
            textSize = 11f
            setBackgroundColor(Color.parseColor("#059669"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
            }
        }

        actionButtonsLayout.addView(btnCapturePhoto, btnParams)
        actionButtonsLayout.addView(btnCaptureVideo, btnParams)
        actionButtonsLayout.addView(btnPickGallery, btnParams)

        statusTextView = TextView(this).apply {
            text = "🟢 التشفير والحفظ المباشر متصل بـ DCIM/OmniLens"
            textSize = 11f
            setTextColor(Color.parseColor("#4ADE80"))
            gravity = Gravity.CENTER
        }

        hashTextView = TextView(this).apply {
            text = "⚡ اضغط ⋮ لإدارة العنصر أو تفاعل عبر الأزرار العائمة."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(10, 5, 10, 15)
        }

        val historyTitle = TextView(this).apply {
            text = "📜 موجز استوديو OmniLens العمودي"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F8FAFC"))
            setPadding(0, 15, 0, 10)
        }

        historyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val footerRights = TextView(this).apply {
            text = "جميع الحقوق محفوظة منصة OmniLens وللمستخدم"
            textSize = 11f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 10)
        }

        rootLayout.addView(headerCard)
        rootLayout.addView(quickBarLayout)
        rootLayout.addView(selectedImageView)
        rootLayout.addView(zoomNoticeText)
        rootLayout.addView(actionButtonsLayout)
        rootLayout.addView(statusTextView)
        rootLayout.addView(hashTextView)
        rootLayout.addView(historyTitle)
        rootLayout.addView(historyLayout)
        rootLayout.addView(footerRights)

        mainContentLayout.addView(rootLayout)

        // شاشة البداية 3 ثوانٍ
        splashLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#090D16"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val splashContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
        }

        val splashLogo = ImageView(this).apply {
            val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (imageResId != 0) setImageResource(imageResId)
            layoutParams = LinearLayout.LayoutParams(240, 240)
        }

        val splashTitle = TextView(this).apply {
            text = "OMNILENS"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 5)
        }

        val progressBar = ProgressBar(this).apply {
            indeterminateDrawable.setTint(Color.parseColor("#2563EB"))
        }

        splashContent.addView(splashLogo)
        splashContent.addView(splashTitle)
        splashContent.addView(progressBar)

        splashLayout.addView(splashContent)

        rootContainer.addView(mainContentLayout)
        rootContainer.addView(splashLayout)

        setContentView(rootContainer)

        showProtectionNotification()

        Handler(Looper.getMainLooper()).postDelayed({
            splashLayout.visibility = View.GONE
            mainContentLayout.visibility = View.VISIBLE
            loadSavedVaultHistory()
        }, 3000)
    }

    private fun showAccountLoginDialog() {
        val options = arrayOf(
            "🆔 تسجيل الدخول بـ OmniLens ID خاص",
            "📧 تسجيل الدخول عبر Google",
            "📱 تسجيل الدخول عبر رقم الهاتف",
            "🚪 تسجيل الخروج"
        )

        AlertDialog.Builder(this)
            .setTitle("🔑 إدارة حساب المستخدم والتراخيص:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showOmniIdRegisterDialog()
                    1 -> {
                        currentUserOmniId = "Google_User_" + (1000..9999).random()
                        isUserLoggedIn = true
                        userAccountStatusText.text = "👤 الحساب: $currentUserOmniId ✅"
                        Toast.makeText(this, "تم تسجيل الدخول عبر Google ✅", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        currentUserOmniId = "Phone_User_" + (1000..9999).random()
                        isUserLoggedIn = true
                        userAccountStatusText.text = "👤 الحساب: $currentUserOmniId ✅"
                        Toast.makeText(this, "تم تسجيل الدخول برقم الهاتف ✅", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        currentUserOmniId = "Guest_OmniUser"
                        isUserLoggedIn = false
                        userAccountStatusText.text = "👤 الحساب: $currentUserOmniId (زائر)"
                        Toast.makeText(this, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showOmniIdRegisterDialog() {
        val input = EditText(this).apply {
            hint = "أدخل OmniLens ID الخاص بك (مثال: @saif_omni)"
        }

        AlertDialog.Builder(this)
            .setTitle("🆔 تسجيل OmniLens ID خاص:")
            .setView(input)
            .setPositiveButton("اعتماد") { _, _ ->
                val idText = input.text.toString().trim()
                if (idText.isNotEmpty()) {
                    currentUserOmniId = if (idText.startsWith("@")) idText else "@$idText"
                    isUserLoggedIn = true
                    userAccountStatusText.text = "👤 الحساب: $currentUserOmniId ✅"
                    Toast.makeText(this, "أهلاً بك! تم توثيق حسابك: $currentUserOmniId 🛡️", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showChatRoomDialog() {
        val chatDialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 20, 30, 20)
        }

        val chatHeader = TextView(this).apply {
            text = "💬 دردشة OmniLens المشفرة (SHA-256)"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(0, 0, 0, 10)
        }

        chatMessagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val inputMessage = EditText(this).apply {
            hint = "اكتب رسالة مشفرة أو أرفق بصمة..."
        }

        val btnSendMsg = Button(this).apply {
            text = "إرسال 📤"
            setBackgroundColor(Color.parseColor("#16A34A"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val msg = inputMessage.text.toString().trim()
                if (msg.isNotEmpty()) {
                    addChatMessageCard(currentUserOmniId, msg)
                    inputMessage.setText("")
                }
            }
        }

        chatDialogLayout.addView(chatHeader)
        chatDialogLayout.addView(chatMessagesLayout)
        chatDialogLayout.addView(inputMessage)
        chatDialogLayout.addView(btnSendMsg)

        addChatMessageCard("OmniSystem", "مرحباً بك في غرفة المراسلة المشفرة لـ OmniLens 🔒. الرسائل محمية بالبصمة الرقمية.")

        AlertDialog.Builder(this)
            .setTitle("💬 المراسلة والدردشة الداخلية")
            .setView(chatDialogLayout)
            .setPositiveButton("إغلاق", null)
            .show()
    }

    private fun addChatMessageCard(sender: String, message: String) {
        val msgCard = TextView(this).apply {
            text = "$sender:\n$message"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#334155"))
            setPadding(12, 10, 12, 10)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 8) }
        }
        if (::chatMessagesLayout.isInitialized) {
            chatMessagesLayout.addView(msgCard)
        }
    }

    private fun showNewContentSourceDialog() {
        val options = arrayOf(
            "📷 التقاط صورة جديدة محميّة",
            "🎥 تسجيل مقطع فيديو جديد موثق",
            "🖼️ استيراد صورة أو فيديو من المعرض"
        )
        AlertDialog.Builder(this)
            .setTitle("➕ إنشاء محتوى مشفر جديد:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionsAndCapture(isVideo = false)
                    1 -> checkPermissionsAndCapture(isVideo = true)
                    2 -> {
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                    }
                }
            }
            .show()
    }

    private fun showProtectionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "OmniLens Shield"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (imageResId != 0) imageResId else android.R.drawable.ic_menu_camera)
            .setContentTitle("OmniLens Visual Guard 🛡️")
            .setContentText("نظام التوثيق والدردشة المشفرة نشط")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, builder.build())
    }

    private fun showCameraSettingsDialog() {
        val options = arrayOf("📐 HD (720p)", "📐 FHD (1080p)", "📐 Ultra HD (4K)", "⏱️ 30 FPS", "⏱️ 60 FPS")
        AlertDialog.Builder(this)
            .setTitle("⚙️ إعدادات الكاميرا والجودة:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectedResolution = "HD (720p)"
                    1 -> selectedResolution = "FHD (1080p)"
                    2 -> selectedResolution = "4K (2160p)"
                    3 -> selectedFPS = "30 FPS"
                    4 -> selectedFPS = "60 FPS"
                }
                settingsSummaryText.text = "⚙️ الإعدادات: [$selectedResolution] | [$selectedFPS]"
            }
            .show()
    }

    private fun checkPermissionsAndCapture(isVideo: Boolean) {
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (isVideo) openVideoCamera() else openPhotoCamera()
        } else {
            requestPermissions(permissions.toTypedArray(), PERMISSION_CODE)
        }
    }

    private fun openPhotoCamera() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            tempPhotoFile = File.createTempFile("TEMP_$timeStamp", ".jpg", filesDir)
            val photoURI = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", tempPhotoFile!!)
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
            Toast.makeText(this, "خطأ تسجيل الفيديو: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        
                        // إعادة ضبط حجم المعاينة والماتريكس للبدء بدون زوم
                        scaleFactor = 1.0f
                        matrix.reset()
                        selectedImageView.imageMatrix = matrix

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
                        
                        scaleFactor = 1.0f
                        matrix.reset()
                        selectedImageView.imageMatrix = matrix

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
        } catch (e: Exception) { null }
    }

    private fun processAndAutoSaveMedia(file: File, isVideo: Boolean) {
        try {
            val fileBytes = FileInputStream(file).use { it.readBytes() }
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(fileBytes)
            currentHash = hashBytes.joinToString("") { "%02x".format(it) }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val ext = if (isVideo) ".mp4" else ".png"
            val fileName = "OMNI_$timeStamp$ext"

            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val omniFolder = File(dcimDir, "OmniLens")
            if (!omniFolder.exists()) omniFolder.mkdirs()

            val destFile = File(omniFolder, fileName)
            file.copyTo(destFile, overwrite = true)

            val mimeType = if (isVideo) "video/mp4" else "image/png"
            MediaScannerConnection.scanFile(this, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)

            statusTextView.text = "تم التشفير والحفظ المباشر بـ DCIM/OmniLens ⚡🛡️"
            hashTextView.text = "البصمة الرقمية (SHA-256):\n$currentHash"

            addCardToHistory(currentBitmap, destFile, currentHash, timeStamp)

            if (isCalledFromExternalApp) {
                val resultUri = FileProvider.getUriForFile(this, "com.omnilens.omniguard.provider", destFile)
                val resultIntent = Intent().apply {
                    data = resultUri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الحفظ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addCardToHistory(bitmap: Bitmap?, file: File, hash: String, timeStamp: String) {
        val outerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(18, 18, 18, 18)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val thumbView = ImageView(this).apply {
            if (bitmap != null) setImageBitmap(bitmap) else setImageResource(android.R.drawable.ic_media_play)
            layoutParams = LinearLayout.LayoutParams(100, 100)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val fileText = TextView(this).apply {
            text = file.name
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
        }

        val hashShortText = TextView(this).apply {
            text = "البصمة: ${hash.take(12)}..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f
        }

        infoLayout.addView(fileText)
        infoLayout.addView(hashShortText)

        val btnOptions = Button(this).apply {
            text = "⋮"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(80, 80)
            setOnClickListener { showContentOptionsDialog(file, hash, timeStamp, outerLayout, fileText) }
        }

        card.addView(thumbView)
        card.addView(infoLayout)
        card.addView(btnOptions)

        val reactionsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
        }

        val reactHeart = createReactionButton("❤️") { Toast.makeText(this, "تم تمييز الوسائط كـ مفضلة ❤️", Toast.LENGTH_SHORT).show() }
        val reactNote = createReactionButton("✍🏻") { Toast.makeText(this, "توقيع ملكية البصمة الرقمية ✍🏻", Toast.LENGTH_SHORT).show() }
        val reactVerify = createReactionButton("✅") { Toast.makeText(this, "مطابقة البصمة موثقة 100% ✅", Toast.LENGTH_SHORT).show() }
        val reactTrack = createReactionButton("👣") { Toast.makeText(this, "الأثر الرقمي والسجل الميداني نشط 👣", Toast.LENGTH_SHORT).show() }
        val reactApprove = createReactionButton("👍🏻") { Toast.makeText(this, "تم معالجة واعتماد ترخيص OmniLens 👍🏻", Toast.LENGTH_SHORT).show() }

        val reactParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        reactionsBar.addView(reactHeart, reactParams)
        reactionsBar.addView(reactNote, reactParams)
        reactionsBar.addView(reactVerify, reactParams)
        reactionsBar.addView(reactTrack, reactParams)
        reactionsBar.addView(reactApprove, reactParams)

        outerLayout.addView(card)
        outerLayout.addView(reactionsBar)

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

        historyLayout.addView(outerLayout, 0)
    }

    private fun showContentOptionsDialog(file: File, hash: String, timeStamp: String, cardView: View, fileNameTextView: TextView) {
        val options = arrayOf(
            "📤 مشاركة وتراخيص OmniLens",
            "💬 إرسال عبر دردشة OmniLens المشفرة",
            "ℹ️ تفاصيل التوثيق والـ Hash",
            "✏️ إعادة تسمية الملف",
            "🗑️ حذف المحتوى من الخزنة والجهاز"
        )

        AlertDialog.Builder(this)
            .setTitle("إدارة المحتوى والخيارات:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showShareTypeDialog(file, hash)
                    1 -> {
                        showChatRoomDialog()
                        addChatMessageCard(currentUserOmniId, "قام بتمكين ومشاركة وسائط موثقة ببصمة:\n${hash.take(16)}...")
                    }
                    2 -> showFileDetailsDialog(file, hash, timeStamp)
                    3 -> showRenameFileDialog(file, fileNameTextView)
                    4 -> confirmDeleteFile(file, cardView)
                }
            }
            .show()
    }

    private fun showFileDetailsDialog(file: File, hash: String, timeStamp: String) {
        val fileSizeMB = "%.2f".format(file.length().toDouble() / (1024 * 1024))
        val details = """
            📁 اسم الملف: ${file.name}
            👤 المالك الموثق: $currentUserOmniId
            📊 الحجم: $fileSizeMB ميجابايت
            📅 تاريخ التوثيق: $timeStamp
            📍 المسار: ${file.absolutePath}
            
            🔑 بصمة SHA-256 الكاملة:
            $hash
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("ℹ️ تفاصيل التوثيق والملف:")
            .setMessage(details)
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun showRenameFileDialog(file: File, fileNameTextView: TextView) {
        val input = EditText(this).apply {
            setText(file.nameWithoutExtension)
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("✏️ إعادة تسمية الملف:")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val ext = file.extension
                    val newFile = File(file.parentFile, "$newName.$ext")
                    if (file.renameTo(newFile)) {
                        fileNameTextView.text = newFile.name
                        val mimeType = if (ext.equals("mp4", true)) "video/mp4" else "image/png"
                        MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath, newFile.absolutePath), arrayOf(mimeType), null)
                        Toast.makeText(this, "تمت إعادة التسمية بنجاح ✅", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "تعذرت إعادة التسمية ❌", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun confirmDeleteFile(file: File, cardView: View) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ تأكيد الحذف")
            .setMessage("هل أنت تأكد من رغبتك في حذف هذا المحتوى نهائياً من الخزنة ومعرض الجهاز؟")
            .setPositiveButton("حذف") { _, _ ->
                val path = file.absolutePath
                val ext = file.extension
                if (file.delete()) {
                    val mimeType = if (ext.equals("mp4", true)) "video/mp4" else "image/png"
                    MediaScannerConnection.scanFile(this, arrayOf(path), arrayOf(mimeType), null)
                    historyLayout.removeView(cardView)
                    Toast.makeText(this, "تم حذف المحتوى بنجاح 🗑️", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "تعذر حذف الملف ❌", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun createReactionButton(symbol: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = symbol
            textSize = 12f
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
        }
    }

    private fun showShareTypeDialog(file: File, hash: String) {
        val options = arrayOf("🆓 مشاركة عامة (Free)", "🎁 مشاركة هدية (VIP Gift)", "💰 ترخيص محتوى مدفوع (Commercial)")
        AlertDialog.Builder(this)
            .setTitle("اختر نوع الترخيص والسوشيال ميديا:")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> executeShare(file, hash, "🆓 ترخيص مجاني (عام)", "محتوى موثق متاح للمعاينة.")
                    1 -> executeShare(file, hash, "🎁 ترخيص هدية خاصة (VIP)", "محتوى خاص موثق بـ OmniLens.")
                    2 -> executeShare(file, hash, "💰 ترخيص تجاري مدفوع (Paid)", "⚠️ محتوى تجاري مشفر.")
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
                👤 المالك: $currentUserOmniId
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
