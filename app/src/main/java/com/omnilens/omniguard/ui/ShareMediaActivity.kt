package com.omnilens.omniguard.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.omnilens.omniguard.security.OmniWatermarkEngine
import java.io.File
import java.io.FileOutputStream

class ShareMediaActivity : AppCompatActivity() {

    private var imageUriToShare: Uri? = null

    // اختيار جهة الاتصال لاستخراج الهاتف والإيميل
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
        
        // استقبال URI الصورة المراد توثيقها ومشاركتها
        imageUriToShare = intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if (imageUriToShare != null) {
            openContactPicker()
        } else {
            Toast.makeText(this, "لم يتم تحديد أي صورة للمشاركة", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        selectContactLauncher.launch(intent)
    }

    private fun extractContactInfoAndShare(contactUri: Uri) {
        var phone = "غير محدد"
        var email = "غير محدد"

        val cursor = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val contactId = it.getString(idIndex)

                // 1. استخراج رقم الهاتف
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

                // 2. استخراج البريد الإلكتروني
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

        // معالجة الصورة وتوثيقها بشرائط OmniLens
        processAndSendCertifiedImage(phone, email)
    }

    private fun processAndSendCertifiedImage(
        phone: String,
        email: String,
        licenseType: String = "ترخيص تجاري مدفوع",
        isPrivateMode: Boolean = false
    ) {
        try {
            val inputStream = contentResolver.openInputStream(imageUriToShare!!)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // توليد الصورة الموثقة بختم OmniLens الموحد
            val certifiedBitmap = OmniWatermarkEngine.createCertifiedImage(
                originalBitmap = originalBitmap,
                recipientPhone = phone,
                recipientEmail = email,
                licenseType = licenseType,
                isPrivateMode = isPrivateMode
            )

            // حفظ الصورة المؤقتة للمشاركة عبر FileProvider
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

            // فتح قائمة المشاركة المباشرة (WhatsApp / Telegram / إلخ)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "مشاركة الصورة الموثقة عبر OmniLens"))
            finish()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "حدث خطأ أثناء معالجة الصورة", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
