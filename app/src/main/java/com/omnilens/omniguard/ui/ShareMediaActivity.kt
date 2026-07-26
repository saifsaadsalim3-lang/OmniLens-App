package com.omnilens.omniguard.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.omnilens.omniguard.security.OmniWatermarkEngine
import java.io.File
import java.io.FileOutputStream

class ShareMediaActivity : AppCompatActivity() {

    private var selectedImagePath: String? = null

    private val selectContactLauncher = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        contactUri?.let { uri ->
            val recipientPhone = extractPhoneNumber(uri) ?: "غير محدد"
            processAndShare(recipientPhone)
        } ?: finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedImagePath = intent.getStringExtra("EXTRA_IMAGE_PATH")
        
        selectContactLauncher.launch(null)
    }

    private fun extractPhoneNumber(contactUri: Uri): String? {
        val cursor = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val hasPhone = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                if (hasPhone == "1") {
                    val phones = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )
                    phones?.use { pCursor ->
                        if (pCursor.moveToFirst()) {
                            return pCursor.getString(pCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }
                }
            }
        }
        return null
    }

    private fun processAndShare(recipientPhone: String) {
        val path = selectedImagePath
        if (path == null) {
            finish()
            return
        }
        val originalBitmap = BitmapFactory.decodeFile(path) ?: run {
            finish()
            return
        }

        val certifiedBitmap = OmniWatermarkEngine.createCertifiedImage(
            originalBitmap = originalBitmap,
            recipientIdentifier = recipientPhone
        )

        val cacheFile = File(cacheDir, "omnilens_share_${System.currentTimeMillis()}.png")
        FileOutputStream(cacheFile).use { out ->
            certifiedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val imageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", cacheFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "مشاركة المحتوى الموثق عبر:"))
        finish()
    }
}
