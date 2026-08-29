package tw.nekomimi.nekogram;

import org.telegram.messenger.BuildConfig;

import tw.nekomimi.nekogram.helpers.UserHelper;

public class Extra {

    public static int APP_ID = BuildConfig.API_ID != 0 ? BuildConfig.API_ID : 4;
    public static String APP_HASH = (BuildConfig.API_HASH != null && !BuildConfig.API_HASH.isEmpty() && !"null".equals(BuildConfig.API_HASH)) ? BuildConfig.API_HASH : "014b35b6184100b085b0d0572f9b5103";
    public static String SENTRY_DSN = BuildConfig.SENTRY_DSN != null && !"null".equals(BuildConfig.SENTRY_DSN) ? BuildConfig.SENTRY_DSN : "";

    public static boolean FORCE_ANALYTICS = false;

    private static final UserHelper.BotInfo HELPER_BOT = new UserHelper.BotInfo() {
        @Override
        public long getId() {
            return BuildConfig.HELPER_BOT_ID;
        }

        @Override
        public String getUsername() {
            return BuildConfig.HELPER_BOT_USERNAME;
        }
    };

    public static UserHelper.BotInfo getHelperBot() {
        if (BuildConfig.HELPER_BOT_USERNAME == null || "null".equals(BuildConfig.HELPER_BOT_USERNAME)) {
            return null;
        }
        return HELPER_BOT;
    }

    public static boolean isDirectApp() {
        return true;
    }

    public static boolean isTrustedBot(long id) {
        return id != 0 && id == BuildConfig.HELPER_BOT_ID;
    }
}
