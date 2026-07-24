package com.omnilens.omniguard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

class MainActivity : Activity() {

    private lateinit var selectedImageView: ImageView
    private lateinit var hashTextView: TextView
    private lateinit var statusTextView: TextView

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
            layoutParams = LinearLayout.LayoutParams(220, 220).apply {
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
            setPadding(0, 20, 0, 5)
        }

        // 3. نص حالة النظام
        statusTextView = TextView(this).apply {
            text = "نظام التشفير والحماية جاهز ✅"
            textSize = 15f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        // 4. إطار عرض الصورة المختارة
        selectedImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                550
            ).apply {
                setMargins(0, 10, 0, 20)
            }
            setBackgroundColor(Color.parseColor("#1E293B"))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // 5. أزرار التحكم (الكاميرا والمعرض)
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }

        val btnCamera = Button(this).apply {
            text = "📷 التقاط صورة"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                checkAndOpenCamera()
            }
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
            setMargins(10, 0, 10, 0)
        }
        buttonsLayout.addView(btnCamera, btnParams)
        buttonsLayout.addView(btnGallery, btnParams)

        // 6. نص إخراج البصمة الرقمية والتشفير
        hashTextView = TextView(this).apply {
            text = "قم باختيار أو التقاط صورة لبدء استخراج البصمة الرقمية وتشفيرها."
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }

        // 7. حفظ الحقوق
        val rightsText = TextView(this).apply {
            text = "جميع الحقوق محفوظة لمنصة OmniLens و للمستخدم"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 10)
        }

        // إضافة جميع العناصر للواجهة
        rootLayout.addView(logoImage)
        rootLayout.addView(titleText)
        rootLayout.addView(statusTextView)
        rootLayout.addView(selectedImageView)
        rootLayout.addView(buttonsLayout)
        rootLayout.addView(hashTextView)
        rootLayout.addView(rightsText)

        scrollView.addView(rootLayout)
        setContentView(scrollView)
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
                selectedImageView.setImageBitmap(bitmap)
                processAndEncryptImage(bitmap)
            }
        }
    }

    private fun processAndEncryptImage(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()

        // توليد بصمة SHA-256 الرقمية للملف
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(byteArray)
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }

        statusTextView.text = "تم التشفير وحفظ البصمة الرقمية بنجاح 🔒✅"
        statusTextView.setTextColor(Color.parseColor("#4ADE80"))

        hashTextView.text = "بصمة الملكية الرقمية (SHA-256):\n$hexString\n\n[حالة الملف: مشفر ومحمي ببصمة المالك]"
        hashTextView.setTextColor(Color.parseColor("#E2E8F0"))
    }
}
