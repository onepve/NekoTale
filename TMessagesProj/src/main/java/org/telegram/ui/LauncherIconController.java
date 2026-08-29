package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        try {
            for (LauncherIcon icon : LauncherIcon.values()) {
                if (isEnabled(icon)) {
                    return;
                }
            }

            setIcon(LauncherIcon.DEFAULT);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public static boolean isEnabled(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
            return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
        } catch (Throwable ignore) {
            return icon == LauncherIcon.DEFAULT;
        }
    }

    public static void setIcon(LauncherIcon icon) {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            PackageManager pm = ctx.getPackageManager();
            for (LauncherIcon i : LauncherIcon.values()) {
                pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconDefault),
        AQUA("AquaIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconPremium),
        TURBO("TurboIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconTurbo),
        NOX("NoxIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconNox),
        MERIO("MerioIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconMerio),
        RAINBOW("RainbowIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconRainbow),
        SCHOOL("OldSchoolIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconOldSchool),
        MUSHEEN("MusheenIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconMusheen),
        SPACE("SpaceIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconSpace),
        CLOUD("CloudIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconCloud),
        NEON("NeonIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconNeon),
        MATERIAL("MaterialIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconMaterial);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
