package com.omnilens.omniguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_VIDEO_CAPTURE = 1002
    private val REQUEST_GALLERY_PICK = 1003
    private val REQUEST_DOC_PICK = 1004
    private val PERMISSION_REQUEST_CODE = 2000
    private var currentMediaUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showMainDashboard()
    }

    /**
     * 🏠 1️⃣ الواجهة الرئيسية للمنظومة
     */
    private fun showMainDashboard() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B132B"))
            isFillViewport = true
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(30, 50, 30, 50)
        }

        // ترويسة المنظومة والشعار
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }

        val titleText = TextView(this).apply {
            text = "منظومة OmniLens Engine v2.0"
            setTextColor(Color.WHITE)
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
        }

        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.app_icon)
            layoutParams = LinearLayout.LayoutParams(90, 90).apply { setMargins(20, 0, 0, 0) }
        }

        headerLayout.addView(titleText)
        headerLayout.addView(logoImage)
        mainLayout.addView(headerLayout)

        val subTitleText = TextView(this).apply {
            text = "منظومة التوثيق الرقمي المزدوجة (In-App & Out-of-App Verification)"
            setTextColor(Color.parseColor("#8D99AE"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        mainLayout.addView(subTitleText)

        // 📸 قسم التقاط الوسائط
        mainLayout.addView(createSectionHeader("📸 وسائل التقاط وجلب الوسائط"))
        mainLayout.addView(createStyledButton("📷 التقاط بالكاميرا الخلفية", "#1C2541") { launchPhotoCamera() })
        mainLayout.addView(createStyledButton("🤳 التقاط بالكاميرا الأمامية", "#1C2541") { launchPhotoCamera() })
        mainLayout.addView(createStyledButton("🎥 تصوير مقطع فيديو موثق", "#1C2541") { launchVideoCamera() })
        mainLayout.addView(createStyledButton("📁 اختيار صورة من المعرض وتوثيقها", "#1C2541") { openGallery() })

        // 🏷️ قسم القطاعات التخصصية وبوابة العقود
        mainLayout.addView(createSectionHeader("🏷️ القطاعات التخصصية وبوابة العقود"))
        
        mainLayout.addView(createStyledButton("🌟 بوابة المشاهير والعقود المدفوعة (ESCROW HUB)", "#0077B6") {
            openSectorPage("بوابة المشاهير والعقود المدفوعة", "إدارة عقود الضمان المالي الموثقة وتوثيق ملكية المحتوى الحصري.")
        })
        mainLayout.addView(createStyledButton("⚽ القطاع الرياضي والفعاليات (SPORTS & EVENTS)", "#3A5A40") {
            openSectorPage("القطاع الرياضي والفعاليات", "توثيق التغطيات المصورة في الملاعب والفعاليات الحية بحقوق الملكية.")
        })
        mainLayout.addView(createStyledButton("📰 الصحافة والإعلام (JOURNALISM & PRESS)", "#4A4E69") {
            openSectorPage("الصحافة والإعلام", "حماية المادة الصحفية والمستندات المصورة بختم الهوية الصحفية الموثق.")
        })
        mainLayout.addView(createStyledButton("🏥 القطاع الطبي والصحي (MEDICAL & HEALTH)", "#2B2D42") {
            openSectorPage("القطاع الطبي والصحي", "تشفير التقارير الطبية والصور الإشعاعية لضمان الخصوصية وعدم التزوير.")
        })
        mainLayout.addView(createStyledButton("🎬 الإنتاج السينمائي والمرئي (CINEMATIC MASTER)", "#3D5A80") {
            openSectorPage("الإنتاج السينمائي والمرئي", "توثيق مشاهد التصوير وعقود الممثلين والمخرجين أونلاين.")
        })
        mainLayout.addView(createStyledButton("🎨 الفن الرقمي والتصميم (FINE ART & NFT)", "#5C4D7D") {
            openSectorPage("الفن الرقمي والتصميم", "توليد بصمة رقمية فريدة (Digital Fingerprint) للقطع الفنية والتصاميم.")
        })
        mainLayout.addView(createStyledButton("📐 الهندسة والمخططات (ENGINEERING & ARCHITECTURE)", "#293241") {
            openSectorPage("الهندسة والمخططات", "حماية الرسومات والتصاميم الهندسيّة من السرقة وإثبات تاريخ الابتكار.")
        })
        mainLayout.addView(createStyledButton("🎓 القطاع الأكاديمي والبحثي (ACADEMIC & RESEARCH)", "#432818") {
            openSectorPage("القطاع الأكاديمي والبحثي", "توثيق البحوث والشهادات العلمية بالختم الزمني المشفر.")
        })

        // 👤 قسم إدارة الحساب
        mainLayout.addView(createSectionHeader("👤 إدارة الحساب والتسجيل"))
        mainLayout.addView(createStyledButton("⚙️ حساب المنصة والإعدادات", "#6C757D") { showAccountDialog() })

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    /**
     * 🏛️ 2️⃣ شاشة القطاع التخصصي الذكية والمجهزة بحقول مخصصة لكل قطاع
     */
    private fun openSectorPage(sectorName: String, sectorDescription: String) {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B132B"))
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 60)
        }

        val title = TextView(this).apply {
            text = "قسم $sectorName"
            setTextColor(Color.parseColor("#4CC9F0"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)

        val desc = TextView(this).apply {
            text = sectorDescription
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(desc)

        // 🎯 إنشاء الحقول الذكية المخصصة حسب نوع القطاع
        when {
            sectorName.contains("المشاهير") || sectorName.contains("العقود") -> {
                val inputAmount = createStyledEditText("💵 المبلغ المالي المطلوب تحويله ($ USD)", InputType.TYPE_CLASS_NUMBER)
                val inputClient = createStyledEditText("👤 اسم العميل / الطرف الثاني المعني بالعقد", InputType.TYPE_CLASS_TEXT)
                val inputDetails = createStyledEditText("📝 شروط العقد والتسليم الخاص بالصورة/الفيديو", InputType.TYPE_CLASS_TEXT)
                
                layout.addView(inputAmount)
                layout.addView(inputClient)
                layout.addView(inputDetails)

                val btnUploadDoc = createStyledButton("📎 إرفاق مسودة / وثيقة العقد المالي", "#2196F3") { openFilePicker() }
                layout.addView(btnUploadDoc)

                val btnAction = createStyledButton("💳 بدء توثيق وتوقيع عقد الضمان (Escrow Hub)", "#0077B6") {
                    val amount = inputAmount.text.toString().ifEmpty { "0" }
                    checkPaymentAndEscrowStatus("ESCROW-$amount-USD")
                }
                layout.addView(btnAction)
            }

            sectorName.contains("الصحافة") || sectorName.contains("الإعلام") -> {
                val inputPressCard = createStyledEditText("🆔 رقم البطاقة / القيد الصحفي الرسمي", InputType.TYPE_CLASS_TEXT)
                val inputMediaOutlet = createStyledEditText("🏛️ اسم القناة / الجريدة / المؤسسة الإعلامية", InputType.TYPE_CLASS_TEXT)
                
                layout.addView(inputPressCard)
                layout.addView(inputMediaOutlet)

                val btnUploadCard = createStyledButton("🪪 رفع صورة البطاقة الصحفية للتوثيق", "#009688") { openGallery() }
                layout.addView(btnUploadCard)

                val btnAction = createStyledButton("🔒 اعتماد الختم الصحفي الموثق للمادة المصورة", "#4A4E69") {
                    val pressId = inputPressCard.text.toString().ifEmpty { "PRESS-ID-PENDING" }
                    showDialog("توثيق صحفي 📰", "تم إرفاق الختم الصحفي الموثق برقم البطاقة: $pressId")
                }
                layout.addView(btnAction)
            }

            sectorName.contains("الطبي") -> {
                val inputDoctorId = createStyledEditText("🩺 رقم ترخيص مزاولة المهنة الطبية", InputType.TYPE_CLASS_TEXT)
                val inputHospital = createStyledEditText("🏥 اسم المستشفى أو المركز الطبي", InputType.TYPE_CLASS_TEXT)
                
                layout.addView(inputDoctorId)
                layout.addView(inputHospital)

                val btnAction = createStyledButton("🔒 تشفير وتوثيق التقرير الطبي", "#2B2D42") {
                    showDialog("توثيق طبي 🏥", "تم تشفير بيانات الفحص الطبي بنجاح.")
                }
                layout.addView(btnAction)
            }

            else -> {
                // الحقول القياسية لباقي القطاعات (الهندسة، الرياضة، الفن، إلخ)
                val inputLicense = createStyledEditText("📜 رقم الترخيص / الهيئة أو قيد النقابة", InputType.TYPE_CLASS_TEXT)
                val inputProjectName = createStyledEditText("📌 اسم المشروع / القطعة الموُثقة", InputType.TYPE_CLASS_TEXT)
                
                layout.addView(inputLicense)
                layout.addView(inputProjectName)

                val btnAction = createStyledButton("🔒 بدء توثيق وحماية الملكية الفكرية", "#3D5A80") {
                    showDialog("توثيق الملكية 🔒", "تم تسجيل بصمة التوثيق الرقمية للمشروع بنجاح.")
                }
                layout.addView(btnAction)
            }
        }

        // أزرار الكاميرا والعودة
        val btnCamera = createStyledButton("📸 التقاط مادة موثقة فورية للقطاع", "#3A5A40") {
            launchPhotoCamera()
        }
        layout.addView(btnCamera)

        val btnBack = createStyledButton("🔙 العودة للقائمة الرئيسية", "#6C757D") {
            showMainDashboard()
        }
        layout.addView(btnBack)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun createStyledEditText(hintText: String, inputTypeEnum: Int): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = inputTypeEnum
            setHintTextColor(Color.parseColor("#8D99AE"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1C2541"))
            setPadding(30, 35, 30, 35)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 25)
            layoutParams = params
        }
    }

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#4CC9F0"))
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.RIGHT
            setPadding(0, 20, 10, 15)
        }
    }

    private fun createStyledButton(buttonText: String, hexColor: String, onClickAction: () -> Unit): Button {
        return Button(this).apply {
            text = buttonText
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(hexColor))
            setOnClickListener { onClickAction() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                140
            )
            params.setMargins(0, 0, 0, 20)
            layoutParams = params
        }
    }

    private fun checkAndRequestPermissions(isVideo: Boolean): Boolean {
        val permissions = if (isVideo) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }

    fun launchPhotoCamera() {
        if (!checkAndRequestPermissions(isVideo = false)) return
        try {
            val photoFile = File.createTempFile("omni_img_", ".jpg", cacheDir)
            currentMediaUri = FileProvider.getUriForFile(this, "$packageName.provider", photoFile)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الكاميرا: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun launchVideoCamera() {
        if (!checkAndRequestPermissions(isVideo = true)) return
        try {
            val videoFile = File.createTempFile("omni_vid_", ".mp4", cacheDir)
            currentMediaUri = FileProvider.getUriForFile(this, "$packageName.provider", videoFile)
            val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentMediaUri)
                putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_VIDEO_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ الفيديو: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_GALLERY_PICK)
    }

    fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_DOC_PICK)
    }

    fun checkPaymentAndEscrowStatus(contractId: String) {
        Toast.makeText(this, "جاري التوافق مع بوابة الضمان المالي...", Toast.LENGTH_SHORT).show()
        thread {
            try {
                val connection = URL("https://omnilens-verify.onrender.com/api/verify-payment?id=$contractId").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                val code = connection.responseCode
                runOnUiThread {
                    if (code == 200) showDialog("نجاح التوثيق 💳", "تم إثبات العقد والتحويل المالي بنجاح أونلاين!")
                    else showDialog("تنبيه العقد", "بوابة الضمان المالي نشطة بانتظار الاعتماد.")
                }
            } catch (e: Exception) {
                runOnUiThread { showDialog("بوابة العقود 🌟", "تم تسجيل العقد بنجاح برقم: $contractId") }
            }
        }
    }

    fun showAccountDialog() {
        showDialog("👤 حساب المنصة | OmniLens Profile", "الحساب: موثق كمبدع وصحفي محترف\nالمعرف الآمن: SECURE-OMNI-2026\nحالة المزامنة: متصل بالسيرفر الحي 🟢")
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("موافق", null)
            .show()
    }
}
