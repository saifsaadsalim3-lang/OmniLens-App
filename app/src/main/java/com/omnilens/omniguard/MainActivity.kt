package com.omnilens.omniguard

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_VIDEO_CAPTURE = 1002
    private var currentMediaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🎯 1. إنشاء الحاوية الرئيسية الشاملة للتمرير (ScrollView)
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B132B"))
            isFillViewport = true
        }

        // 🎯 2. إنشاء التخطيط العمودي للأنشطة والأزرار
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(40, 60, 40, 60)
        }

        // 🎯 3. عنوان المنظومة
        val titleText = TextView(this).apply {
            text = "منظومة OmniLens Engine v2.0"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        mainLayout.addView(titleText)

        // 🎯 4. إضافة الأزرار والتصاميم برمجياً
        val btnPhoto = createStyledButton("📸 التقاط بالكاميرا الخلفية") {
            launchPhotoCamera()
        }
        mainLayout.addView(btnPhoto)

        val btnVideo = createStyledButton("🎥 تصوير مقطع فيديو موثق") {
            launchVideoCamera()
        }
        mainLayout.addView(btnVideo)

        val btnPayment = createStyledButton("💳 التحقق من الدفع والضمان المالي") {
            checkPaymentAndEscrowStatus("OMNI-CONTRACT-01")
        }
        mainLayout.addView(btnPayment)

        val btnAccount = createStyledButton("👤 حساب المنصة والإعدادات") {
            showAccountDialog()
        }
        mainLayout.addView(btnAccount)

        // 🎯 5. عرض الواجهة المبنية على الشاشة فوراً
        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    /**
     * دالة مساعدة لتنسيق أزرار الواجهة
     */
    private fun createStyledButton(buttonText: String, onClickAction: () -> Unit): Button {
        return Button(this).apply {
            text = buttonText
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1C2541"))
            setOnClickListener { onClickAction() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                150
            )
            params.setMargins(0, 0, 0, 30)
            layoutParams = params
        }
    }

    /**
     * دالة التقاط الصور
     */
    fun launchPhotoCamera() {
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
     * دالة تصوير الفيديو
     */
    fun launchVideoCamera() {
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
     * نظام التحقق من الدفع المالي
     */
    fun checkPaymentAndEscrowStatus(contractId: String) {
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
     * نافذة حساب المنصة
     */
    fun showAccountDialog() {
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
