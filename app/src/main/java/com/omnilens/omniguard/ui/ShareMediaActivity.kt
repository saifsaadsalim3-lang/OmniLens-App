package com.omnilens.app

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareMediaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // الشاشة الرئيسية للبرنامج عند الفتح المباشر
        showMainAppHub()
    }

    private fun showMainAppHub() {
        val hubOptions = arrayOf(
            "📷 التقاط صورة (الكاميرا الخلفية)",
            "🤳 التقاط صورة (الكاميرا الأمامية)",
            "📁 اختيار ملف من المعرض",
            "🛡️ توثيق ترخيص OmniLens (الأكواد الأربعة)"
        )

        AlertDialog.Builder(this)
            .setTitle("منظومة OmniLens Engine — المركز الرئيسي")
            .setItems(hubOptions) { _, which ->
                when (which) {
                    0 -> openCamera(isFront = false)
                    1 -> openCamera(isFront = true)
                    2 -> openGallery()
                    3 -> showLicenseSelectionDialog()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun showLicenseSelectionDialog() {
        val licenseTypes = arrayOf(
            "🟢 ترخيص تجاري مدفوع",
            "🔵 إهداء خاص",
            "🔴 ملكية خاصة (يمنع النشر)",
            "⚪ ترخيص مجاني (استخدام عام)"
        )

        AlertDialog.Builder(this)
            .setTitle("اختر نوع التوثيق — OmniLens")
            .setItems(licenseTypes) { _, which ->
                val selectedType = licenseTypes[which]
                Toast.makeText(this, "تم اعتماد: $selectedType", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("إلغاء") { _, _ -> showMainAppHub() }
            .show()
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
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح الكاميرا: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح المعرض: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
