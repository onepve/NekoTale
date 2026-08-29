package tw.nekomimi.nekogram.helpers;

import android.app.Application;
import android.content.SharedPreferences;

import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.HashMap;

public class AnalyticsHelper {
    private static SharedPreferences preferences;
    public static boolean sendBugReport = false;
    public static boolean analyticsDisabled = true;
    public static String userId = null;

    public static void start(Application application) {
        try {
            preferences = application.getSharedPreferences("nekoanalytics", Application.MODE_PRIVATE);
            analyticsDisabled = true;
            sendBugReport = false;
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static void trackFragmentLifecycle(String lifecycle, BaseFragment fragment) {
    }

    public static void trackEvent(String event, HashMap<String, String> map) {
    }

    public static boolean isSettingsAvailable() {
        return false;
    }

    public static void toggleSendBugReport() {
        sendBugReport = false;
    }

    public static void setSendBugReport(boolean value) {
        sendBugReport = false;
    }

    public static void setAnalyticsDisabled() {
        analyticsDisabled = true;
    }

    public static void setAnalyticsDisabled(boolean value) {
        analyticsDisabled = true;
    }
}
