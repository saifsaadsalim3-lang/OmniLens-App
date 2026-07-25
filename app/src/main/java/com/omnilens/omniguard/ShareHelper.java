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
     * دالة مشاركة الصور مع وضع اللوجو الشفاف في منتصف الصورة وإظهار الإشعار
     */
    public static void shareProtectedFile(Context context, File sourceFile, String mimeType) {
        if (sourceFile == null || !sourceFile.exists()) return;

        // 1️⃣ دمج اللوجو في المنتصف بشرط الشفافية المتوسطة
        File watermarkedFile = addCenterWatermark(context, sourceFile);

        // 2️⃣ إطلاق إشعار التوثيق
        showProtectionNotification(context);

        // 3️⃣ إنشاء الـ URI الآمن بـ FileProvider
        String authority = context.getPackageName() + ".fileprovider";
        Uri contentUri = FileProvider.getUriForFile(context, authority, watermarkedFile);

        // 4️⃣ تجهيز أمر المشاركة
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType != null ? mimeType : "image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        
        // نص التوثيق المرفق
        String shareCaption = "🔒 منصة OmniLens للحماية وتوثيق الوسائط\n-----------------------------------\n© جميع الحقوق محفوظة لمنصة OmniLens وللمستخدم.";
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareCaption);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 5️⃣ فتح قائمة المشاركة
        Intent chooserIntent = Intent.createChooser(shareIntent, "مشاركة صورة محمية عبر OmniLens");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(chooserIntent);
    }

    /**
     * دالة رسم اللوجو في المنتصف بشفافية متوسطة (Center Watermark)
     */
    private static File addCenterWatermark(Context context, File sourceFile) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);
            if (bitmap == null) return sourceFile;

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            // جلب الشعار app_icon
            Drawable watermarkDrawable = ContextCompat.getDrawable(context, R.drawable.app_icon);
            if (watermarkDrawable == null) return sourceFile;

            // 📐 حساب أبعاد اللوجو (ليكون حوالي 35% من عرض الصورة)
            int watermarkWidth = (int) (mutableBitmap.getWidth() * 0.35);
            int intrinsicWidth = watermarkDrawable.getIntrinsicWidth();
            int intrinsicHeight = watermarkDrawable.getIntrinsicHeight();

            int watermarkHeight;
            if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                watermarkHeight = (watermarkWidth * intrinsicHeight) / intrinsicWidth;
            } else {
                watermarkHeight = watermarkWidth; // أبعاد افتراضية حماية للأنواع الأخرى
            }

            // 🎯 وضع اللوجو في المكرز/المنتصف بالضبط
            int left = (mutableBitmap.getWidth() - watermarkWidth) / 2;
            int top = (mutableBitmap.getHeight() - watermarkHeight) / 2;

            watermarkDrawable.setBounds(left, top, left + watermarkWidth, top + watermarkHeight);
            watermarkDrawable.setAlpha(120); // 👈 شفافية متوسطة (حوالي 47%)
            watermarkDrawable.draw(canvas);

            // 💾 حفظ الصورة المائية في مجلد الكاش
            File cacheFile = new File(context.getCacheDir(), "omnilens_watermarked_" + System.currentTimeMillis() + ".png");
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return cacheFile;

        } catch (Exception e) {
            e.printStackTrace();
            return sourceFile;
        }
    }

    /**
     * إظهار إشعار حماية وتوثيق الوسائط
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
