package com.omnilens.omniguard.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ShareMediaActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra("FILE_PATH")
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "تعذر تحديد مسار الملف للمشاركة", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(this, "الملف غير موجود في الخزنة", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        shareFile(file)
    }

    private fun shareFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "com.omnilens.omniguard.provider",
                file
            )

            val mimeType = if (file.name.endsWith(".mp4", ignoreCase = true)) "video/*" else "image/*"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "وسائط موثقة عبر OmniLens")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "تم توثيق هذه الوسائط وحمايتها بواسطة منظومة OmniLens Ecosystem.\nرقم التوثيق: OMNILENS-IP-INV-SAIF-2026"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "مشاركة الوسائط الموثقة عبر:"))
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "حدث خطأ أثناء المشاركة: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
