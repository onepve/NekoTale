package tw.nekomimi.nekogram.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CatFoodLog {
    private static final String TAG = "CatFood";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    public static synchronized void d(String message) {
        log("DEBUG", message);
    }

    public static synchronized void i(String message) {
        log("INFO", message);
    }

    public static synchronized void w(String message) {
        log("WARN", message);
    }

    public static synchronized void e(String message) {
        log("ERROR", message);
    }

    public static synchronized void e(String message, Throwable t) {
        String stack = Log.getStackTraceString(t);
        log("ERROR", message + "\n" + stack);
    }

    private static synchronized void log(String level, String message) {
        String timestamp = sdf.format(new Date());
        String logLine = String.format("[%s] [%s] %s\n", timestamp, level, message);

        Log.i(TAG, logLine.trim());

        Context context = ApplicationLoader.applicationContext;

        // 1. 优先直接写入公共存储 /sdcard/Download/NekoTale/log/catfood.log
        try {
            File pubDownload = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (pubDownload != null) {
                File nekoDir = new File(pubDownload, "NekoTale/log");
                if (!nekoDir.exists()) {
                    nekoDir.mkdirs();
                }
                File logFile = new File(nekoDir, "catfood.log");
                try (FileWriter fw = new FileWriter(logFile, true)) {
                    fw.write(logLine);
                    fw.flush();
                }
            }
        } catch (Throwable t) {
            // 2. Android 10+ MediaStore 兜底写入
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, "catfood_append.log");
                    values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/NekoTale/log");
                    Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wa")) {
                            if (os != null) {
                                os.write(logLine.getBytes(StandardCharsets.UTF_8));
                                os.flush();
                            }
                        }
                    }
                } catch (Throwable ignore) {}
            }
        }
    }
}
