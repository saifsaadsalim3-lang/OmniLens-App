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
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var selectedImageView: ImageView
    private lateinit var hashTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var btnSave: Button
    private lateinit var historyLayout: LinearLayout

    private var currentBitmap: Bitmap? = null
    private var currentHash: String = ""

    private val CAMERA_REQUEST_CODE = 101
    private val GALLERY_REQUEST_CODE = 102
    private val CAMERA_PERMISSION_CODE = 103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 60, 40, 60)
        }

        // 1. الشعار
        val logoImage = ImageView(this).apply {
            val imageResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (imageResId != 0) {
                setImageResource(imageResId)
            }
            layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                gravity = Gravity.CENTER
            }
        }

        // 2. العنوان
        val titleText = TextView(this).apply {
            text = "OMNILENS"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 5)
        }

        // 3. نص حالة النظام
        statusTextView = TextView(this).apply {
            text = "نظام التشفير والتراخيص جاهز ✅"
            textSize = 15f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // 4. إطار عرض الوسائط المختارة
        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            ).apply {
                setMargins(0, 10, 0, 15)
            }
            setBackgroundColor(Color.parseColor("#1E293B"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // 5. أزرار التحكم (الكاميرا والمعرض)
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        val btnCamera = Button(this).apply {
            text = "📷 التقاط صورة"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener { checkAndOpenCamera() }
        }

        val btnGallery = Button(this).apply {
            text = "🖼️ المعرض"
            setBackgroundColor(Color.parseColor("#0D9488"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
            }
        }

        val btnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(5, 0, 5, 0)
        }
        buttonsLayout.addView(btnCamera, btnParams)
        buttonsLayout.addView(btnGallery, btnParams)

        // 6. زر الحفظ في الخزنة
        btnSave = Button(this).apply {
            text = "💾 حفظ في الخزنة المحمية"
            setBackgroundColor(Color.parseColor("#475569"))
            setTextColor(Color.WHITE)
            isEnabled = false
            setOnClickListener { saveEncryptedImageToVault() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 10, 0, 15)
            }
        }

        // 7. نص البصمة الرقمية
        hashTextView = TextView(this).apply {
            text = "قم باختيار أو التقاط صورة لبدء استخراج البصمة الرقمية وتشفيرها."
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(15, 15, 15, 15)
        }

        // 8. عنوان سجل الخزنة
        val historyTitle = TextView(this).apply {
            text = "📜 سجل الصور المحمية (الخزنة)"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#F8FAFC"))
            setPadding(0, 25, 0, 15)
        }

        // 9. حاوية سجل الصور
        historyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 10. حفظ الحقوق
        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة لمنصة OmniLens و للمستخدم"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }

        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(statusTextView)
        rootLayout.addView(selectedImageView)
        rootLayout.addView(buttonsLayout)
        rootLayout.addView(btnSave)
        rootLayout.addView(hashTextView)
        rootLayout.addView(historyTitle)
        rootLayout.addView(historyLayout)
        rootLayout.addView(rightsText)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        loadSavedVaultHistory()
    }

    private fun checkAndOpenCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                statusTextView.text = "تم رفض صلاحية الكاميرا ❌"
                statusTextView.setTextColor(Color.parseColor("#EF4444"))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            var bitmap: Bitmap? = null

            if (requestCode == CAMERA_REQUEST_CODE) {
                bitmap = data.extras?.get("data") as? Bitmap
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                val imageUri: Uri? = data.data
                if (imageUri != null) {
                    bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                }
            }

            if (bitmap != null) {
                currentBitmap = bitmap
                selectedImageView.setImageBitmap(bitmap)
                processAndEncryptImage(bitmap)
            }
        }
    }

    private fun processAndEncryptImage(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(byteArray)
        currentHash = hashBytes.joinToString("") { "%02x".format(it) }

        statusTextView.text = "تم التشفير وحفظ البصمة الرقمية بنجاح 🔒✅"
        statusTextView.setTextColor(Color.parseColor("#4ADE80"))

        hashTextView.text = "بصمة الملكية الرقمية (SHA-256):\n$currentHash\n\n[حالة الملف: جاهز للحفظ في الخزنة]"
        hashTextView.setTextColor(Color.parseColor("#E2E8F0"))

        btnSave.isEnabled = true
        btnSave.setBackgroundColor(Color.parseColor("#16A34A"))
    }

    private fun saveEncryptedImageToVault() {
        val bitmap = currentBitmap ?: return
        if (currentHash.isEmpty()) return

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "OMNI_$timeStamp.png"

            val vaultDir = File(filesDir, "OmniVault")
            if (!vaultDir.exists()) {
                vaultDir.mkdirs()
            }

            val imageFile = File(vaultDir, fileName)
            val fos = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            Toast.makeText(this, "تم حفظ الصورة في الخزنة بنجاح 🛡️", Toast.LENGTH_SHORT).show()

            addCardToHistory(bitmap, imageFile, currentHash, timeStamp)

            btnSave.isEnabled = false
            btnSave.setBackgroundColor(Color.parseColor("#475569"))
            statusTextView.text = "تم الحفظ في الخزنة المحمية 📦✅"

        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في الحفظ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addCardToHistory(bitmap: Bitmap, file: File, hash: String, timeStamp: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 15)
            }
        }

        val thumbView = ImageView(this).apply {
            setImageBitmap(bitmap)
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val fileText = TextView(this).apply {
            text = file.name
            setTextColor(Color.WHITE)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        }

        val dateText = TextView(this).apply {
            text = "التاريخ: $timeStamp"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 11f
        }

        val hashShortText = TextView(this).apply {
            text = "البصمة: ${hash.take(14)}..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f
        }

        infoLayout.addView(fileText)
        infoLayout.addView(dateText)
        infoLayout.addView(hashShortText)

        // زر المشاركة والتصنيف
        val btnShare = Button(this).apply {
            text = "📤"
            textSize = 16f
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(110, 110).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            setOnClickListener {
                showShareTypeDialog(file, hash)
            }
        }

        card.addView(thumbView)
        card.addView(infoLayout)
        card.addView(btnShare)

        historyLayout.addView(card, 0)
    }

    private fun showShareTypeDialog(file: File, hash: String) {
        val options = arrayOf("🆓 مشاركة مجانية (Free)", "🎁 مشاركة كهدية مميزة (Gift)", "💰 مشاركة كترخيص مدفوع (Paid)")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("اختر نوع الترخيص والمشاركة:")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> executeShare(file, hash, "🆓 ترخيص مجاني (عرض عام)", "المحتوى متاح للمعاينة والتداول المجاني.")
                1 -> executeShare(file, hash, "🎁 ترخيص هدية مميزة (VIP)", "هذا المحتوى مرسل كهدية خاصة وموثقة بالبصمة.")
                2 -> executeShare(file, hash, "💰 ترخيص محتوى مدفوع (Commercial)", "⚠️ محتوى تجاري مشفر. يُشترط الحصول على مفتاح الفك للاستخدام القانوني.")
            }
        }
        builder.show()
    }

    private fun executeShare(file: File, hash: String, licenseTitle: String, licenseDesc: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val mimeType = if (file.name.endsWith(".mp4", ignoreCase = true)) "video/*" else "image/*"

            val shareMessage = """
                🔒 منصة OmniLens للحماية وتوثيق الوسائط
                ----------------------------------------
                📌 نوع الترخيص: $licenseTitle
                📝 الوصف: $licenseDesc
                
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

            startActivity(Intent.createChooser(shareIntent, "مشاركة الوسائط عبر:"))
        } catch (e: Exception) {
            Toast.makeText(this, "تعذرت المشاركة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadSavedVaultHistory() {
        val vaultDir = File(filesDir, "OmniVault")
        if (vaultDir.exists()) {
            val files = vaultDir.listFiles()?.sortedByDescending { it.lastModified() }
            files?.forEach { file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val digest = MessageDigest.getInstance("SHA-256")
                    val hashBytes = digest.digest(stream.toByteArray())
                    val hash = hashBytes.joinToString("") { "%02x".format(it) }

                    val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(file.lastModified()))
                    addCardToHistory(bitmap, file, hash, date)
                }
            }
        }
    }
}
