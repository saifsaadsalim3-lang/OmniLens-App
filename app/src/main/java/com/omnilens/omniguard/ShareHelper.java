package com.omnilens.omniguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class ShareHelper {

    /**
     * دالة مشاركة الصور مع إضافة العلامة المائية وإظهار إشعار الحماية
     * @param context سياق التطبيق
     * @param sourceFile الملف المراد مشاركته
     * @param mimeType نوع الملف (مثل "image/*")
     */
    public static void shareProtectedFile(Context context, File sourceFile, String mimeType) {
        if (sourceFile == null || !sourceFile.exists()) return;

        // 1️⃣ إضافة اللوجو الشفاف كعلامة مائية على الصورة
        File watermarkedFile = addWatermarkToImage(context, sourceFile);

        // 2️⃣ إطلاق إشعار التوثيق والحماية
        showProtectionNotification(context);

        // 3️⃣ إنشاء Content URI باستخدام FileProvider
        String authority = context.getPackageName() + ".fileprovider";
        Uri contentUri = FileProvider.getUriForFile(context, authority, watermarkedFile);

        // 4️⃣ تجهيز Intent المشاركة
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType != null ? mimeType : "image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 5️⃣ فتح نافذة الاختيار للمستخدم
        Intent chooserIntent = Intent.createChooser(shareIntent, "مشاركة صورة محمية عبر OmniLens");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(chooserIntent);
    }

    /**
     * دالة رسم الشعار الشفاف فوق الصورة وتوليد نسخة محمية
     */
    private static File addWatermarkToImage(Context context, File sourceFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath());
            if (bitmap == null) return sourceFile;

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            // جلب الشعار app_icon من مجلد drawable
            Drawable watermarkDrawable = ContextCompat.getDrawable(context, R.drawable.app_icon);
            if (watermarkDrawable == null) return sourceFile;

            // تحديد حجم الشعار (ربع عرض الصورة)
            int watermarkWidth = mutableBitmap.getWidth() / 4;
            int watermarkHeight = (watermarkWidth * watermarkDrawable.getIntrinsicHeight()) / watermarkDrawable.getIntrinsicWidth();

            // تحديد موقع الشعار (أسفل اليمين)
            int left = mutableBitmap.getWidth() - watermarkWidth - 40;
            int top = mutableBitmap.getHeight() - watermarkHeight - 40;

            watermarkDrawable.setBounds(left, top, left + watermarkWidth, top + watermarkHeight);
            watermarkDrawable.setAlpha(140); // درجة الشفافية (140 من 255 - شفافية متوسطة)
            watermarkDrawable.draw(canvas);

            // حفظ الصورة المؤقتة في مجلد Cache
            File cacheFile = new File(context.getCacheDir(), "omnilens_protected_" + System.currentTimeMillis() + ".png");
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return cacheFile;
        } catch (Exception e) {
            e.printStackTrace();
            return sourceFile; // إرجاع الصورة الأصلية في حال حدوث أي خطأ لضمان استمرار المشاركة
        }
    }

    /**
     * دالة إرسال إشعار أمان OmniLens
     */
    private static void showProtectionNotification(Context context) {
        String channelId = "omnilens_security_channel";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "OmniLens Security Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("تنبيهات حماية وتوثيق الوسائط");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🛡️ OmniLens Security")
                .setContentText("هذه الصورة محمية وموثقة عن طريق OmniLens")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
