package tw.nekomimi.nekogram.helpers;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.CameraScanActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CatFoodHelper {

    public static void showCatFoodDialog(Activity activity, BaseFragment fragment, Theme.ResourcesProvider resourcesProvider) {
        if (activity == null) {
            return;
        }

        Context context = activity;
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.FeedCatFood));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(24), dp(12), dp(24), dp(16));

        TextView hintTextView = new TextView(context);
        hintTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        hintTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2, resourcesProvider));
        hintTextView.setText(LocaleController.getString(R.string.FeedCatFoodHint));
        container.addView(hintTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        editText.setHintTextColor(Theme.getColor(Theme.key_groupcreate_hintText, resourcesProvider));
        editText.setHint("https://...");
        editText.setFocusable(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setMaxLines(4);
        editText.setPadding(dp(14), dp(10), dp(14), dp(10));
        editText.setCursorWidth(1.5f);
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));

        GradientDrawable fieldBackground = new GradientDrawable();
        fieldBackground.setCornerRadius(dp(12));
        fieldBackground.setColor(Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider), 0.06f));
        editText.setBackground(fieldBackground);

        // Load saved subscription URL if available
        String savedFeedUrl = MessagesController.getGlobalMainSettings().getString("cat_food_url", "");
        if (!TextUtils.isEmpty(savedFeedUrl)) {
            editText.setText(savedFeedUrl);
            editText.setSelection(savedFeedUrl.length());
        }

        // Auto paste from clipboard if available on dialog opening and editText is empty
        try {
            if (TextUtils.isEmpty(editText.getText())) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence clipText = clip.getItemAt(0).getText();
                        if (clipText != null) {
                            String s = clipText.toString().trim();
                            if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("tg://") || s.startsWith("socks5://") || s.startsWith("vmess://") || s.startsWith("ss://")) {
                                editText.setText(s);
                                editText.setSelection(s.length());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        container.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

        // Quick action buttons: [扫码] + [粘贴]
        LinearLayout actionsLayout = new LinearLayout(context);
        actionsLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionsLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout scanBtn = createActionButton(context, R.drawable.msg_qrcode, LocaleController.getString(R.string.FeedCatFoodScan), resourcesProvider);
        LinearLayout pasteBtn = createActionButton(context, R.drawable.msg_copy, LocaleController.getString(R.string.FeedCatFoodPaste), resourcesProvider);

        actionsLayout.addView(scanBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, 0, 0, dp(8), 0));
        actionsLayout.addView(pasteBtn, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f, dp(8), 0, 0, 0));

        container.addView(actionsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, dp(12), 0, 0));
        builder.setView(container);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        scanBtn.setOnClickListener(v -> {
            if (dialogRef[0] != null) {
                try {
                    dialogRef[0].dismiss();
                } catch (Exception ignore) {}
            }
            if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA}, 34);
                return;
            }
            CameraScanActivity.showAsSheet(activity, true, CameraScanActivity.TYPE_QR, new CameraScanActivity.CameraScanActivityDelegate() {
                @Override
                public void didFindQr(String scanned) {
                    if (!TextUtils.isEmpty(scanned)) {
                        processFeed(activity, scanned);
                    }
                }
            });
        });

        pasteBtn.setOnClickListener(v -> {
            try {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence clipText = clip.getItemAt(0).getText();
                        if (clipText != null) {
                            String s = clipText.toString().trim();
                            if (!TextUtils.isEmpty(s)) {
                                editText.setText(s);
                                editText.setSelection(s.length());
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}
            Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodEmpty), Toast.LENGTH_SHORT).show();
        });

        builder.setPositiveButton(LocaleController.getString(R.string.FeedCatFoodConfirm), (dialogInterface, i) -> {
            String input = editText.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodEmpty), Toast.LENGTH_SHORT).show();
                return;
            }
            processFeed(context, input);
        });

        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        dialogRef[0] = dialog;
        if (fragment != null) {
            fragment.showDialog(dialog);
        } else {
            dialog.show();
        }
    }

    private static LinearLayout createActionButton(Context context, int iconRes, String text, Theme.ResourcesProvider resourcesProvider) {
        LinearLayout button = new LinearLayout(context);
        button.setOrientation(LinearLayout.HORIZONTAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(8), dp(12), dp(8));

        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(8), Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider), 0.08f), Theme.multAlpha(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider), 0.16f)));

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        button.addView(icon, LayoutHelper.createLinear(20, 20, Gravity.CENTER_VERTICAL, 0, 0, dp(6), 0));

        TextView tv = new TextView(context);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        tv.setText(text);
        tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        button.addView(tv, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        return button;
    }

    public static void processFeed(Context context, String rawInput) {
        if (context == null || TextUtils.isEmpty(rawInput)) {
            return;
        }

        if (rawInput.startsWith("http://") || rawInput.startsWith("https://")) {
            MessagesController.getGlobalMainSettings().edit().putString("cat_food_url", rawInput.trim()).apply();
        }

        AlertDialog progressDialog = new AlertDialog(context, 3);
        progressDialog.setMessage(LocaleController.getString(R.string.FeedCatFoodLoading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Utilities.globalQueue.postRunnable(() -> {
            boolean success = false;
            String fetchedData = rawInput;

            try {
                if (rawInput.startsWith("http://") || rawInput.startsWith("https://")) {
                    URL url = new URL(rawInput);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "ClashMeta/1.18.0 (NekoTale)");
                    int respCode = conn.getResponseCode();
                    if (respCode >= 200 && respCode < 400) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        reader.close();
                        fetchedData = sb.toString().trim();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }

            ArrayList<SharedConfig.ProxyInfo> parsedList = parseProxies(fetchedData);
            if (!parsedList.isEmpty()) {
                success = true;
                applyProxy(parsedList.get(0), parsedList);
            }

            final boolean finalSuccess = success;
            final ArrayList<SharedConfig.ProxyInfo> finalParsedList = parsedList;
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {}

                if (finalSuccess) {
                    if (finalParsedList.size() > 1) {
                        showNodeSelectionDialog(context, finalParsedList);
                    } else {
                        Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodSuccess), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodFailed), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    public static void showNodeSelectionDialog(Context context) {
        ArrayList<SharedConfig.ProxyInfo> list = SharedConfig.proxyList;
        if (list == null || list.isEmpty()) {
            Toast.makeText(context, LocaleController.getString(R.string.NoProxyFound), Toast.LENGTH_SHORT).show();
            return;
        }
        showNodeSelectionDialog(context, list);
    }

    public static void showNodeSelectionDialog(Context context, ArrayList<SharedConfig.ProxyInfo> list) {
        if (context == null || list == null || list.isEmpty()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.FeedCatFoodSelectNode));

        CharSequence[] items = new CharSequence[list.size()];
        int selectedIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            SharedConfig.ProxyInfo info = list.get(i);
            String name = !TextUtils.isEmpty(info.username) && !info.username.contains("=") ? info.username : (info.address + ":" + info.port);
            items[i] = name;
            if (SharedConfig.currentProxy == info || (SharedConfig.currentProxy != null && TextUtils.equals(SharedConfig.currentProxy.address, info.address) && SharedConfig.currentProxy.port == info.port)) {
                selectedIndex = i;
            }
        }

        builder.setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
            if (which >= 0 && which < list.size()) {
                applyProxy(list.get(which), list);
                Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodSuccess), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    private static ArrayList<SharedConfig.ProxyInfo> parseProxies(String data) {
        ArrayList<SharedConfig.ProxyInfo> list = new ArrayList<>();
        if (TextUtils.isEmpty(data)) {
            return list;
        }

        String content = data.trim();
        // Try Base64 decoding if content looks encoded
        try {
            if (!content.contains("://") && !content.contains("proxies:") && content.length() > 20) {
                byte[] decoded = Base64.decode(content, Base64.DEFAULT);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                if (decodedStr.contains("://") || decodedStr.contains("proxies:") || decodedStr.contains("server")) {
                    content = decodedStr;
                }
            }
        } catch (Exception ignore) {}

        String[] lines = content.split("[\\r\\n]+");
        for (String line : lines) {
            line = line.trim();
            if (TextUtils.isEmpty(line) || line.startsWith("#")) {
                continue;
            }

            SharedConfig.ProxyInfo info = parseSingleLine(line);
            if (info != null && !TextUtils.isEmpty(info.address) && info.port > 0) {
                list.add(info);
            }
        }

        // Also check Clash YAML format if line parsing returned nothing
        if (list.isEmpty() && content.contains("server:") && content.contains("port:")) {
            Pattern p = Pattern.compile("server:\\s*[\"']?([^\"'\\s,]+)[\"']?[\\s\\S]*?port:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            while (m.find()) {
                String address = m.group(1);
                int port = Utilities.parseInt(m.group(2));
                if (!TextUtils.isEmpty(address) && port > 0) {
                    list.add(new SharedConfig.ProxyInfo(address, port, "", "", ""));
                }
            }
        }

        return list;
    }

    private static SharedConfig.ProxyInfo parseSingleLine(String line) {
        try {
            if (line.startsWith("tg://proxy?") || line.startsWith("tg:proxy?") || line.startsWith("https://t.me/proxy?") || line.startsWith("http://t.me/proxy?")) {
                Uri uri = Uri.parse(line.replace("tg:proxy?", "https://t.me/proxy?").replace("tg://proxy?", "https://t.me/proxy?"));
                String server = uri.getQueryParameter("server");
                int port = Utilities.parseInt(uri.getQueryParameter("port"));
                String secret = uri.getQueryParameter("secret");
                if (!TextUtils.isEmpty(server) && port > 0) {
                    return new SharedConfig.ProxyInfo(server, port, "", "", secret != null ? secret : "");
                }
            } else if (line.startsWith("tg://socks?") || line.startsWith("tg:socks?") || line.startsWith("https://t.me/socks?") || line.startsWith("http://t.me/socks?")) {
                Uri uri = Uri.parse(line.replace("tg:socks?", "https://t.me/socks?").replace("tg://socks?", "https://t.me/socks?"));
                String server = uri.getQueryParameter("server");
                int port = Utilities.parseInt(uri.getQueryParameter("port"));
                String user = uri.getQueryParameter("user");
                String pass = uri.getQueryParameter("pass");
                if (!TextUtils.isEmpty(server) && port > 0) {
                    return new SharedConfig.ProxyInfo(server, port, user != null ? user : "", pass != null ? pass : "", "");
                }
            } else if (line.startsWith("socks5://") || line.startsWith("socks://") || line.startsWith("http://") || line.startsWith("https://")) {
                URI uri = new URI(line);
                String host = uri.getHost();
                int port = uri.getPort();
                String userInfo = uri.getUserInfo();
                String user = "";
                String pass = "";
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    user = parts[0];
                    pass = parts[1];
                }
                if (!TextUtils.isEmpty(host) && port > 0) {
                    return new SharedConfig.ProxyInfo(host, port, user, pass, "");
                }
            } else if (line.contains(":") && !line.contains("://")) {
                // Support raw formats: server:port:secret or server:port:user:pass or server:port
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    String host = parts[0].trim();
                    int port = Utilities.parseInt(parts[1].trim());
                    if (!TextUtils.isEmpty(host) && port > 0) {
                        if (parts.length == 3) {
                            // server:port:secret (MTProto)
                            return new SharedConfig.ProxyInfo(host, port, "", "", parts[2].trim());
                        } else if (parts.length == 4) {
                            // server:port:user:pass (Socks5)
                            return new SharedConfig.ProxyInfo(host, port, parts[2].trim(), parts[3].trim(), "");
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static void applyProxy(SharedConfig.ProxyInfo current, ArrayList<SharedConfig.ProxyInfo> allList) {
        if (current == null) {
            return;
        }

        for (SharedConfig.ProxyInfo item : allList) {
            SharedConfig.addProxy(item);
        }
        SharedConfig.currentProxy = SharedConfig.addProxy(current);

        // Mutual exclusion: configure the single active connection cleanly
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", true);
        editor.putString("proxy_ip", current.address);
        editor.putInt("proxy_port", current.port);
        if (!TextUtils.isEmpty(current.secret)) {
            editor.putString("proxy_secret", current.secret);
            editor.remove("proxy_user");
            editor.remove("proxy_pass");
        } else {
            editor.remove("proxy_secret");
            if (TextUtils.isEmpty(current.username)) {
                editor.remove("proxy_user");
            } else {
                editor.putString("proxy_user", current.username);
            }
            if (TextUtils.isEmpty(current.password)) {
                editor.remove("proxy_pass");
            } else {
                editor.putString("proxy_pass", current.password);
            }
        }
        editor.commit();

        SharedConfig.saveProxyList();

        ConnectionsManager.setProxySettings(true, current.address, current.port, current.username, current.password, current.secret);

        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            ConnectionsManager.native_resumeNetwork(a, false);
            try {
                ConnectionsManager.getInstance(a).updateDcSettings();
            } catch (Exception ignore) {}
        }

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);
    }
}
