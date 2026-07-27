package com.omnilens.omniguard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

public class ShareHelper {

    public static void shareBitmap(Context context, Bitmap bitmap, String title) {
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            if (!cachePath.exists()) {
                cachePath.mkdirs();
            }

            File file = new File(cachePath, "OmniLens_Secured_" + System.currentTimeMillis() + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                file
            );

            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("image/png");

                Intent chooserIntent = Intent.createChooser(shareIntent, title);
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooserIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
