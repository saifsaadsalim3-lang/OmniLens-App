package com.omnilens.app; // غير اسم الحزمة لما يناسب مشروعك

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

public class ShareHelper {

    /**
     * دالة مشاركة الملفات عبر FileProvider
     * @param context سياق التطبيق
     * @param fileToShare الملف المراد مشاركته (صورة/فيديو)
     * @param mimeType نوع الملف (مثلاً: "image/*" أو "video/*")
     */
    public static void shareFile(Context context, File fileToShare, String mimeType) {
        if (fileToShare == null || !fileToShare.exists()) {
            return;
        }

        // 1. إنشاء Content URI الآمن باستخدام FileProvider
        String authority = context.getPackageName() + ".fileprovider";
        Uri contentUri = FileProvider.getUriForFile(context, authority, fileToShare);

        // 2. تجهيز Intent المشاركة
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType != null ? mimeType : "*/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

        // 3. منح صلاحية القراءة للتطبيقات الأخرى (مهمة جداً للـ FileProvider)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // 4. إطلاق نافذة الاختيار (Chooser)
        Intent chooserIntent = Intent.createChooser(shareIntent, "مشاركة الوسائط عبر OmniLens");
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(chooserIntent);
    }
}
