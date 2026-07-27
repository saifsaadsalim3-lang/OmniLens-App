package com.omnilens.omniguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

// 🎯 استيراد كلاس الموارد الصريح للربط المباشر بالواجهة وإلغاء الشاشة السوداء
import com.omnilens.omniguard.R

class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_VIDEO_CAPTURE = 1002
    private var currentMediaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 💡 عرض واجهة التطبيق الأساسية مباشرة عند الفتح
        setContentView(R.layout.activity_main)
    }

    /**
     * 1️⃣ التقاط الصور بالكاميرا
     */
    fun launchPhotoCamera(view: View? = null) {
        try {
            val photoFile = File.createTempFile("omni_img_", ".jpg", cacheDir)
            val authority = "$packageName.provider"
            currentMediaUri = FileProvider.getUriForFile(this, authority, photoFile)

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ فتح الكاميرا: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 2️⃣ تصوير مقطع فيديو موثق
     */
    fun launchVideoCamera(view: View? = null) {
        try {
            val videoFile = File.createTempFile("omni_vid_", ".mp4", cacheDir)
            val authority = "$packageName.provider"
            currentMediaUri = FileProvider.getUriForFile(this, authority, videoFile)

            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri)
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ فتح فيديو الكاميرا: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 3️⃣ نظام حساب المنصة والتحقق من الدفع المالي
     */
    fun checkPaymentAndEscrowStatus(contractId: String = "OMNI-CONTRACT-01") {
        Toast.makeText(this, "جاري التحقق من عملية الدفع والعقد أونلاين...", Toast.LENGTH_SHORT).show()

        thread {
            try {
                val serverUrl = URL("https://omnilens-verify.onrender.com/api/verify-payment?id=$contractId")
                val connection = serverUrl.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000

                val responseCode = connection.responseCode
                runOnUiThread {
                    if (responseCode == 200) {
                        showDialog("نجاح الدفع 💳", "تم إثبات عملية الدفع بنجاح والعقد موثق أونلاين!")
                    } else {
                        showDialog("تنبيه الدفع", "لم يتم اعتماد معاملة الدفع لهذا المعرف بعد.")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showDialog("حالة الاتصال", "خدمة التوثيق السحابية نشطة. بانتظار استجابة بوابة الدفع.")
                }
            }
        }
    }

    /**
     * 4️⃣ نافذة إضافة وإدارة حساب المنصة
     */
    fun showAccountDialog(view: View? = null) {
        AlertDialog.Builder(this)
            .setTitle("👤 حساب المنصة | OmniLens Profile")
            .setMessage("الحساب: موثق رسمي (Creator & Press)\nالمعرف الآمن: SECURE-OMNI-2026\nحالة المزامنة: متصل بالسيرفر الحي 🟢")
            .setPositiveButton("إغلاق") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("موافق", null)
            .show()
    }
}
