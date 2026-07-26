package com.omnilens.omniguard.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.omnilens.omniguard.security.OmniWatermarkEngine
import java.io.File
import java.io.FileOutputStream

class ShareMediaActivity : AppCompatActivity() {

    private var imageUriToShare: Uri? = null
    private var selectedLicenseType: String = "ترخيص تجاري مدفوع"
    private var isPrivateModeSelected: Boolean = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUriToShare = uri
            showModeSelectionDialog()
        }
    }

    private val selectContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val contactData: Uri? = result.data?.data
            contactData?.let { extractContactInfoAndShare(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // حماية دالة التشغيل بمنظومة try-catch لمنع الانهيار المباشر
        try {
            if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
                imageUriToShare = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                if (imageUriToShare != null) {
                    showModeSelectionDialog()
                } else {
                    Toast.makeText(this, "تعذر قراءة الصورة المشاركة", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                setupMainUI()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "حدث خطأ أثناء تشغيل الواجهة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMainUI() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(60, 60, 60, 60)
        }

        // معالجة استدعاء الأيقونة بشكل آمن لتفادي الانهيار
        val iconView = ImageView(this).apply {
            val iconResId = resources.getIdentifier("app_icon", "drawable", packageName)
            if (iconResId != 0) {
                setImageResource(iconResId)
            } else {
                setImageResource(android.R.drawable.ic_menu_camera)
            }
            layoutParams = LinearLayout.LayoutParams(240, 240).apply {
                bottomMargin = 40
            }
        }

        val titleText = TextView(this).apply {
            text = "OmniLens Engine"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val descText = TextView(this).apply {
            text = "منصة توثيق وتشفير الحقوق الرقمية للمحتوى المرئي"
            setTextColor(Color.parseColor("#AAAAAA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 80)
        }

        val actionButton = Button(this).apply {
            text = "📷 اختيار صورة لتوثيقها"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E88E5"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(40, 30, 40, 30)
            setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }

        rootLayout.addView(iconView)
        rootLayout.addView(titleText)
        rootLayout.addView(descText)
        rootLayout.addView(actionButton)

        setContentView(rootLayout)
    }

    private fun showModeSelectionDialog() {
        val modes = arrayOf(
            "🟢 ترخيص تجاري مدفوع",
            "🔵 إهداء خاص",
            "🔴 ملكية خاصة (يمنع النشر)"
        )

        AlertDialog.Builder(this)
            .setTitle("OmniLens — اختر نوع التوثيق")
            .setItems(modes) { _, which ->
                when (which) {
                    0 -> {
                        selectedLicenseType = "ترخيص تجاري مدفوع"
                        isPrivateModeSelected = false
                        openContactPicker()
                    }
                    1 -> {
                        selectedLicenseType = "إهداء خاص"
                        isPrivateModeSelected = false
                        openContactPicker()
                    }
                    2 -> {
                        selectedLicenseType = "ملكية خاصة (يمنع النشر)"
                        isPrivateModeSelected = true
                        showPrivateModeNoticeDialog()
                    }
                }
            }
            .setNegativeButton("إلغاء") { dialog, _ -> 
                dialog.dismiss()
                if (intent?.action == Intent.ACTION_SEND) finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPrivateModeNoticeDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ تنبيه مهم للملكية الخاصة")
            .setMessage("سيتم طباعة بياناتك كمالك أصلي (رقم الهاتف والبريد الإلكتروني) على الشريط التحذيري لتأكيد ملكيتك وحمايتها من التداول.")
            .setPositiveButton("موافق ومتابعة") { _, _ ->
                openContactPicker()
            }
            .setNegativeButton("تراجع") { _, _ ->
                showModeSelectionDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun openContactPicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            selectContactLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractContactInfoAndShare(contactUri: Uri) {
        var phone = "غير محدد"
        var email = "غير محدد"

        try {
            val cursor = contentResolver.query(contactUri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val contactId = it.getString(idIndex)

                    val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                    val hasPhone = it.getString(hasPhoneIndex)
                    if (hasPhone == "1") {
                        val pCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        pCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                phone = pc.getString(phoneIndex)
                            }
                        }
                    }

                    val eCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    eCursor?.use { ec ->
                        if (ec.moveToFirst()) {
                            val emailIndex = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                            email = ec.getString(emailIndex)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        processAndSendCertifiedImage(phone, email)
    }

    private fun processAndSendCertifiedImage(phone: String, email: String) {
        try {
            val inputStream = contentResolver.openInputStream(imageUriToShare!!)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            val certifiedBitmap = OmniWatermarkEngine.createCertifiedImage(
                originalBitmap = originalBitmap,
                recipientPhone = phone,
                recipientEmail = email,
                licenseType = selectedLicenseType,
                isPrivateMode = isPrivateModeSelected
            )

            val cachePath = File(cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "omnilens_certified_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            certifiedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "مشاركة الصورة الموثقة عبر OmniLens"))

            if (intent?.action == Intent.ACTION_SEND) {
                finish()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "حدث خطأ أثناء معالجة الصورة", Toast.LENGTH_SHORT).show()
        }
    }
}
