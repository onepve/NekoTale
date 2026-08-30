package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CatFoodLog {
    private static final String TAG = "CatFood";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final List<String> memoryLogs = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_MEMORY_LOGS = 500;

    public static void d(String message) {
        log("DEBUG", message);
    }

    public static void i(String message) {
        log("INFO", message);
    }

    public static void w(String message) {
        log("WARN", message);
    }

    public static void e(String message) {
        log("ERROR", message);
    }

    public static void e(String message, Throwable t) {
        String stack = Log.getStackTraceString(t);
        log("ERROR", message + "\n" + stack);
    }

    public static List<String> getLogs() {
        synchronized (memoryLogs) {
            return new ArrayList<>(memoryLogs);
        }
    }

    public static String getLogsText() {
        synchronized (memoryLogs) {
            StringBuilder sb = new StringBuilder();
            for (String l : memoryLogs) {
                sb.append(l);
            }
            return sb.toString();
        }
    }

    private static synchronized void log(String level, String message) {
        String timestamp = sdf.format(new Date());
        String logLine = String.format("[%s] [%s] %s\n", timestamp, level, message);

        Log.i(TAG, logLine.trim());

        // 1. 内存留存用于 UI 弹窗即时查看
        synchronized (memoryLogs) {
            if (memoryLogs.size() >= MAX_MEMORY_LOGS) {
                memoryLogs.remove(0);
            }
            memoryLogs.add(logLine);
        }

        // 2. 尝试写入应用内部存储或外部目录
        try {
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                File internalDir = new File(context.getFilesDir(), "logs");
                if (!internalDir.exists()) {
                    internalDir.mkdirs();
                }
                File intLog = new File(internalDir, "catfood.log");
                try (FileWriter fw = new FileWriter(intLog, true)) {
                    fw.write(logLine);
                    fw.flush();
                }
            }

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
        } catch (Throwable ignore) {}
    }
}
