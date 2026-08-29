package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tw.nekomimi.nekogram.CrashActivity;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "NekoTaleCrash";
    private static CrashHandler instance;
    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context applicationContext;

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new CrashHandler();
            instance.applicationContext = context.getApplicationContext();
            instance.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(instance);
            Log.i(TAG, "CrashHandler installed successfully.");
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        String report = "====== NekoTale Crash Report ======\n"
                + "Time: " + time + "\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ", SDK " + Build.VERSION.SDK_INT + ")\n"
                + "Thread: " + thread.getName() + " (id=" + thread.getId() + ")\n"
                + "Exception: " + throwable.getClass().getName() + ": " + throwable.getMessage() + "\n\n"
                + "Stack Trace:\n" + stackTrace;

        Log.e(TAG, report);

        try {
            if (applicationContext != null) {
                File dir = applicationContext.getExternalFilesDir(null);
                if (dir == null) {
                    dir = applicationContext.getFilesDir();
                }
                if (dir != null) {
                    File crashFile = new File(dir, "crash_dump.log");
                    FileWriter writer = new FileWriter(crashFile, false);
                    writer.write(report);
                    writer.flush();
                    writer.close();
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to write crash dump file", t);
        }

        try {
            if (applicationContext != null) {
                Intent intent = new Intent(applicationContext, CrashActivity.class);
                intent.putExtra("crash_report", report);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                applicationContext.startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
                return;
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start CrashActivity", t);
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}
