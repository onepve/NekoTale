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

import org.json.JSONArray;
import org.json.JSONObject;
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

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CatFoodHelper {

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

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
                            if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("tg://") || s.startsWith("socks5://") || s.startsWith("hysteria2://") || s.startsWith("hy2://") || s.startsWith("vless://") || s.startsWith("vmess://") || s.startsWith("trojan://") || s.startsWith("ss://")) {
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
                        processFeed(activity, scanned.trim());
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
        dialog.show();
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

        final String cleanInput = rawInput.trim();
        CatFoodLog.i("开始喂猫粮, 输入内容长度=" + cleanInput.length() + ", 前缀=" + (cleanInput.length() > 30 ? cleanInput.substring(0, 30) + "..." : cleanInput));

        if (cleanInput.startsWith("http://") || cleanInput.startsWith("https://")) {
            MessagesController.getGlobalMainSettings().edit().putString("cat_food_url", cleanInput).apply();
        }

        AlertDialog progressDialog = new AlertDialog(context, 3);
        progressDialog.setMessage(LocaleController.getString(R.string.FeedCatFoodLoading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Utilities.globalQueue.postRunnable(() -> {
            boolean success = false;
            String fetchedData = cleanInput;

            try {
                if (cleanInput.startsWith("http://") || cleanInput.startsWith("https://")) {
                    CatFoodLog.i("正在通过 OkHttp 请求远程订阅: " + cleanInput);
                    Request req = new Request.Builder()
                            .url(cleanInput)
                            .header("User-Agent", "ClashMeta/1.18.0 (NekoTale)")
                            .header("Accept", "*/*")
                            .build();
                    try (Response response = httpClient.newCall(req).execute()) {
                        CatFoodLog.i("远程订阅响应状态码: " + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            fetchedData = response.body().string().trim();
                            CatFoodLog.i("获取成功, 数据长度: " + fetchedData.length() + " 字符");
                        } else {
                            CatFoodLog.w("远程订阅响应异常: " + response.code() + " " + response.message());
                        }
                    }
                }
            } catch (Exception e) {
                CatFoodLog.e("请求远程订阅抛出异常", e);
                FileLog.e(e);
            }

            ArrayList<SharedConfig.ProxyInfo> parsedList = parseProxies(fetchedData);
            if (parsedList.isEmpty() && !cleanInput.equals(fetchedData)) {
                CatFoodLog.i("尝试直接解析原始输入字符串...");
                parsedList = parseProxies(cleanInput);
            }

            CatFoodLog.i("解析节点总数: " + parsedList.size());
            for (int i = 0; i < parsedList.size(); i++) {
                SharedConfig.ProxyInfo p = parsedList.get(i);
                CatFoodLog.i(String.format("  [%d] 名称: %s | 目标: %s:%d | secret=%s | user=%s", 
                        i + 1, p.username, p.address, p.port, (TextUtils.isEmpty(p.secret) ? "无" : "有"), (TextUtils.isEmpty(p.username) ? "无" : p.username)));
            }

            if (!parsedList.isEmpty()) {
                success = true;
                applyProxy(parsedList.get(0), parsedList);
            } else {
                CatFoodLog.w("未能解析出任何有效代理节点");
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
                        String name = !TextUtils.isEmpty(finalParsedList.get(0).username) ? finalParsedList.get(0).username : (finalParsedList.get(0).address + ":" + finalParsedList.get(0).port);
                        Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodSuccess) + ": " + name, Toast.LENGTH_LONG).show();
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
        for (int i = 0; i < list.size(); i++) {
            SharedConfig.ProxyInfo info = list.get(i);
            String name = !TextUtils.isEmpty(info.username) ? info.username : (info.address + ":" + info.port);
            boolean isCurrent = SharedConfig.currentProxy == info || (SharedConfig.currentProxy != null && TextUtils.equals(SharedConfig.currentProxy.address, info.address) && SharedConfig.currentProxy.port == info.port);
            items[i] = (isCurrent ? "✓ " : "  ") + name;
        }

        builder.setItems(items, (dialog, which) -> {
            if (which >= 0 && which < list.size()) {
                SharedConfig.ProxyInfo chosen = list.get(which);
                applyProxy(chosen, list);
                String chosenName = !TextUtils.isEmpty(chosen.username) ? chosen.username : (chosen.address + ":" + chosen.port);
                Toast.makeText(context, LocaleController.getString(R.string.FeedCatFoodSuccess) + ": " + chosenName, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.show();
    }

    public static ArrayList<SharedConfig.ProxyInfo> parseProxies(String data) {
        ArrayList<SharedConfig.ProxyInfo> list = new ArrayList<>();
        if (TextUtils.isEmpty(data)) {
            return list;
        }

        String content = data.trim();

        // Check if content is Clash YAML
        if (content.contains("proxies:") || (content.contains("server:") && (content.contains("type:") || content.contains("port:")))) {
            ArrayList<SharedConfig.ProxyInfo> yamlList = parseClashYaml(content);
            if (!yamlList.isEmpty()) {
                return yamlList;
            }
        }

        // Check if content is Sing-box JSON
        if (content.startsWith("{") && content.contains("\"outbounds\"")) {
            ArrayList<SharedConfig.ProxyInfo> jsonList = parseSingBoxJson(content);
            if (!jsonList.isEmpty()) {
                return jsonList;
            }
        }

        // Try Base64 decoding if content looks encoded
        try {
            if (!content.contains("://") && !content.contains("proxies:") && content.length() > 16) {
                byte[] decoded = Base64.decode(content, Base64.DEFAULT);
                String decodedStr = new String(decoded, StandardCharsets.UTF_8).trim();
                if (decodedStr.contains("://") || decodedStr.contains("proxies:") || decodedStr.contains("server") || decodedStr.contains("outbounds")) {
                    content = decodedStr;
                    if (content.contains("proxies:") || content.contains("server:")) {
                        ArrayList<SharedConfig.ProxyInfo> yamlList = parseClashYaml(content);
                        if (!yamlList.isEmpty()) {
                            return yamlList;
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        String[] lines = content.split("[\\r\\n]+");
        for (String line : lines) {
            line = line.trim();
            if (TextUtils.isEmpty(line) || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            SharedConfig.ProxyInfo info = parseSingleLine(line);
            if (info != null && !TextUtils.isEmpty(info.address) && info.port > 0) {
                list.add(info);
            }
        }

        return list;
    }

    private static ArrayList<SharedConfig.ProxyInfo> parseClashYaml(String yaml) {
        ArrayList<SharedConfig.ProxyInfo> list = new ArrayList<>();
        try {
            String[] lines = yaml.split("[\\r\\n]+");
            String curName = "";
            String curServer = "";
            int curPort = 0;
            String curPassword = "";
            String curSecret = "";
            String curType = "";

            for (String line : lines) {
                String stripped = line.trim();
                if (stripped.startsWith("#")) continue;

                // Handle inline dict: - { name: "HK", server: "1.1.1.1", port: 443, ... }
                if (stripped.startsWith("-") && stripped.contains("{") && stripped.contains("}")) {
                    String inside = stripped.substring(stripped.indexOf("{") + 1, stripped.lastIndexOf("}"));
                    String[] pairs = inside.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    String inName = "", inServer = "", inPw = "", inSecret = "", inType = "";
                    int inPort = 0;
                    for (String pair : pairs) {
                        String[] kv = pair.split(":", 2);
                        if (kv.length == 2) {
                            String k = kv[0].trim().replace("\"", "").replace("'", "");
                            String v = kv[1].trim().replace("\"", "").replace("'", "");
                            if ("name".equalsIgnoreCase(k)) inName = v;
                            else if ("server".equalsIgnoreCase(k)) inServer = v;
                            else if ("port".equalsIgnoreCase(k)) inPort = Utilities.parseInt(v);
                            else if ("password".equalsIgnoreCase(k) || "uuid".equalsIgnoreCase(k) || "auth_str".equalsIgnoreCase(k)) inPw = v;
                            else if ("secret".equalsIgnoreCase(k)) inSecret = v;
                            else if ("type".equalsIgnoreCase(k)) inType = v;
                        }
                    }
                    if (!TextUtils.isEmpty(inServer) && inPort > 0) {
                        String displayName = !TextUtils.isEmpty(inName) ? inName : (inServer + ":" + inPort);
                        list.add(new SharedConfig.ProxyInfo(inServer, inPort, displayName, inPw, inSecret));
                    }
                    continue;
                }

                if (stripped.startsWith("-")) {
                    if (!TextUtils.isEmpty(curServer) && curPort > 0) {
                        String displayName = !TextUtils.isEmpty(curName) ? curName : (curServer + ":" + curPort);
                        list.add(new SharedConfig.ProxyInfo(curServer, curPort, displayName, curPassword, curSecret));
                    }
                    curName = "";
                    curServer = "";
                    curPort = 0;
                    curPassword = "";
                    curSecret = "";
                    curType = "";
                }

                if (stripped.contains(":")) {
                    String[] kv = stripped.split(":", 2);
                    String k = kv[0].replace("-", "").trim().replace("\"", "").replace("'", "");
                    String v = kv[1].trim().replace("\"", "").replace("'", "");
                    if ("name".equalsIgnoreCase(k)) curName = v;
                    else if ("server".equalsIgnoreCase(k)) curServer = v;
                    else if ("port".equalsIgnoreCase(k)) curPort = Utilities.parseInt(v);
                    else if ("password".equalsIgnoreCase(k) || "uuid".equalsIgnoreCase(k) || "auth_str".equalsIgnoreCase(k)) curPassword = v;
                    else if ("secret".equalsIgnoreCase(k)) curSecret = v;
                    else if ("type".equalsIgnoreCase(k)) curType = v;
                }
            }

            if (!TextUtils.isEmpty(curServer) && curPort > 0) {
                String displayName = !TextUtils.isEmpty(curName) ? curName : (curServer + ":" + curPort);
                list.add(new SharedConfig.ProxyInfo(curServer, curPort, displayName, curPassword, curSecret));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    private static ArrayList<SharedConfig.ProxyInfo> parseSingBoxJson(String jsonStr) {
        ArrayList<SharedConfig.ProxyInfo> list = new ArrayList<>();
        try {
            JSONObject obj = new JSONObject(jsonStr);
            if (obj.has("outbounds")) {
                JSONArray outbounds = obj.getJSONArray("outbounds");
                for (int i = 0; i < outbounds.length(); i++) {
                    JSONObject out = outbounds.getJSONObject(i);
                    String tag = out.optString("tag", "");
                    String server = out.optString("server", "");
                    int port = out.optInt("server_port", 0);
                    String type = out.optString("type", "");
                    String password = out.optString("password", out.optString("uuid", ""));
                    String secret = out.optString("secret", "");

                    if (!TextUtils.isEmpty(server) && port > 0 && !type.equals("direct") && !type.equals("block") && !type.equals("dns")) {
                        String displayName = !TextUtils.isEmpty(tag) ? tag : (server + ":" + port);
                        list.add(new SharedConfig.ProxyInfo(server, port, displayName, password, secret));
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return list;
    }

    public static SharedConfig.ProxyInfo parseSingleLine(String line) {
        try {
            String remark = "";
            if (line.contains("#")) {
                String[] parts = line.split("#", 2);
                line = parts[0].trim();
                try {
                    remark = URLDecoder.decode(parts[1].trim(), "UTF-8");
                } catch (Exception e) {
                    remark = parts[1].trim();
                }
            }

            if (line.startsWith("tg://proxy?") || line.startsWith("tg:proxy?") || line.startsWith("https://t.me/proxy?") || line.startsWith("http://t.me/proxy?")) {
                Uri uri = Uri.parse(line.replace("tg:proxy?", "https://t.me/proxy?").replace("tg://proxy?", "https://t.me/proxy?"));
                String server = uri.getQueryParameter("server");
                int port = Utilities.parseInt(uri.getQueryParameter("port"));
                String secret = uri.getQueryParameter("secret");
                if (!TextUtils.isEmpty(server) && port > 0) {
                    String displayName = !TextUtils.isEmpty(remark) ? remark : (server + ":" + port);
                    return new SharedConfig.ProxyInfo(server, port, displayName, "", secret != null ? secret : "");
                }
            } else if (line.startsWith("tg://socks?") || line.startsWith("tg:socks?") || line.startsWith("https://t.me/socks?") || line.startsWith("http://t.me/socks?")) {
                Uri uri = Uri.parse(line.replace("tg:socks?", "https://t.me/socks?").replace("tg://socks?", "https://t.me/socks?"));
                String server = uri.getQueryParameter("server");
                int port = Utilities.parseInt(uri.getQueryParameter("port"));
                String user = uri.getQueryParameter("user");
                String pass = uri.getQueryParameter("pass");
                if (!TextUtils.isEmpty(server) && port > 0) {
                    String displayName = !TextUtils.isEmpty(remark) ? remark : (server + ":" + port);
                    return new SharedConfig.ProxyInfo(server, port, displayName, pass != null ? pass : "", "");
                }
            } else if (line.startsWith("hysteria2://") || line.startsWith("hy2://")) {
                URI uri = new URI(line);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 443;
                String auth = uri.getUserInfo();
                if (!TextUtils.isEmpty(host) && port > 0) {
                    String displayName = !TextUtils.isEmpty(remark) ? remark : (host + ":" + port);
                    return new SharedConfig.ProxyInfo(host, port, displayName, auth != null ? auth : "", "");
                }
            } else if (line.startsWith("vless://") || line.startsWith("trojan://") || line.startsWith("tuic://")) {
                URI uri = new URI(line);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 443;
                String auth = uri.getUserInfo();
                if (!TextUtils.isEmpty(host) && port > 0) {
                    String displayName = !TextUtils.isEmpty(remark) ? remark : (host + ":" + port);
                    return new SharedConfig.ProxyInfo(host, port, displayName, auth != null ? auth : "", "");
                }
            } else if (line.startsWith("ss://")) {
                String rest = line.substring(5);
                String host = "";
                int port = 8388;
                String password = "";
                if (rest.contains("@")) {
                    String[] parts = rest.split("@", 2);
                    try {
                        String userinfo = new String(Base64.decode(parts[0], Base64.URL_SAFE | Base64.DEFAULT), StandardCharsets.UTF_8);
                        if (userinfo.contains(":")) {
                            password = userinfo.split(":", 2)[1];
                        }
                    } catch (Exception e) {
                        password = parts[0];
                    }
                    if (parts[1].contains(":")) {
                        String[] hp = parts[1].split(":");
                        host = hp[0];
                        port = Utilities.parseInt(hp[1]);
                    }
                } else {
                    try {
                        String dec = new String(Base64.decode(rest, Base64.URL_SAFE | Base64.DEFAULT), StandardCharsets.UTF_8);
                        URI u = new URI("http://" + dec);
                        host = u.getHost();
                        port = u.getPort() > 0 ? u.getPort() : 8388;
                    } catch (Exception ignore) {}
                }
                if (!TextUtils.isEmpty(host) && port > 0) {
                    String displayName = !TextUtils.isEmpty(remark) ? remark : (host + ":" + port);
                    return new SharedConfig.ProxyInfo(host, port, displayName, password, "");
                }
            } else if (line.startsWith("socks5://") || line.startsWith("socks://")) {
                URI uri = new URI(line);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 1080;
                String userInfo = uri.getUserInfo();
                String pass = "";
                if (userInfo != null && userInfo.contains(":")) {
                    pass = userInfo.split(":", 2)[1];
                }
                if (!TextUtils.isEmpty(host) && port > 0) {
                    String displayName = !TextUtils.isEmpty(remark) ? remark : (host + ":" + port);
                    return new SharedConfig.ProxyInfo(host, port, displayName, pass, "");
                }
            } else if (line.contains(":") && !line.contains("://")) {
                String[] parts = line.split(":");
                if (parts.length >= 2) {
                    String host = parts[0].trim();
                    int port = Utilities.parseInt(parts[1].trim());
                    if (!TextUtils.isEmpty(host) && port > 0) {
                        String secret = parts.length >= 3 ? parts[2].trim() : "";
                        String displayName = !TextUtils.isEmpty(remark) ? remark : (host + ":" + port);
                        return new SharedConfig.ProxyInfo(host, port, displayName, "", secret);
                    }
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static void applyProxy(SharedConfig.ProxyInfo current, ArrayList<SharedConfig.ProxyInfo> allList) {
        if (current == null) {
            CatFoodLog.w("applyProxy 传入的 current 为 null");
            return;
        }

        String nodeName = !TextUtils.isEmpty(current.username) ? current.username : (current.address + ":" + current.port);
        CatFoodLog.i(String.format("正在应用节点: [%s] -> %s:%d, secret=%s", nodeName, current.address, current.port, (TextUtils.isEmpty(current.secret) ? "无" : "有")));

        for (SharedConfig.ProxyInfo item : allList) {
            SharedConfig.addProxy(item);
        }
        SharedConfig.currentProxy = SharedConfig.addProxy(current);

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
        CatFoodLog.i("节点已激活, ConnectionsManager代理设置完成, 已通知网络层重连");
    }
}
