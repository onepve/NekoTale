package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CatFoodCoreManager {

    private static final int LOCAL_PORT = 10808;
    private static Process coreProcess = null;

    public static synchronized boolean isRunning() {
        if (coreProcess != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return coreProcess.isAlive();
            } else {
                try {
                    coreProcess.exitValue();
                    return false;
                } catch (IllegalThreadStateException e) {
                    return true;
                }
            }
        }
        return isPortOpen("127.0.0.1", LOCAL_PORT, 200);
    }

    public static synchronized void stopCore() {
        if (coreProcess != null) {
            CatFoodLog.i("正在停止 Mihomo 本地代理内核...");
            try {
                coreProcess.destroy();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    coreProcess.destroyForcibly();
                }
            } catch (Exception e) {
                CatFoodLog.e("停止内核异常", e);
            }
            coreProcess = null;
        }
    }

    public static synchronized boolean startCoreWithNode(Context context, String rawData, String selectedNodeName) {
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context == null) {
            CatFoodLog.e("Context 为空，无法启动内核");
            return false;
        }

        stopCore();

        File binFile = getExecutableBinary(context);
        if (binFile == null || !binFile.exists()) {
            CatFoodLog.e("未找到 Mihomo 内核可执行文件 (libnekocore.so)");
            return false;
        }

        File filesDir = context.getFilesDir();
        File configFile = new File(filesDir, "core_config.yaml");

        try {
            String configContent = generateClashConfig(rawData, selectedNodeName);
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                fos.write(configContent.getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
            CatFoodLog.i("Mihomo 配置文件已生成 (" + configContent.length() + " 字节)");
        } catch (Exception e) {
            CatFoodLog.e("生成内核配置文件失败", e);
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    binFile.getAbsolutePath(),
                    "-d", filesDir.getAbsolutePath(),
                    "-f", configFile.getName()
            );
            pb.directory(filesDir);
            pb.redirectErrorStream(true);

            coreProcess = pb.start();
            CatFoodLog.i("Mihomo 内核进程已启动");

            final Process p = coreProcess;
            Utilities.globalQueue.postRunnable(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 过滤高频常规数据流和初始化细节，只显示关键警告、错误、启动与就绪信息
                        if (line.contains("[TCP]") && line.contains("match Match") && !line.contains("error") && !line.contains("timeout")) {
                            continue;
                        }
                        if (line.contains("Geodata Loader") || line.contains("Geosite Matcher") || line.contains("Sniffer is closed") || line.contains("compatible provider") || line.contains("can't open cache file")) {
                            continue;
                        }
                        if (line.contains("level=warning") || line.contains("level=error") || line.contains("error:") || line.contains("timeout")) {
                            CatFoodLog.w("[Core] " + line);
                        } else {
                            CatFoodLog.i("[Core] " + line);
                        }
                    }
                } catch (Exception ignore) {}
            });

            // 立即将 Telegram 代理指向本地回路 127.0.0.1:10808 (免主线程阻塞)
            applyLocalProxyToTelegram("127.0.0.1", LOCAL_PORT, selectedNodeName);

            // 异步验证端口就绪
            Utilities.globalQueue.postRunnable(() -> {
                for (int i = 0; i < 20; i++) {
                    if (isPortOpen("127.0.0.1", LOCAL_PORT, 200)) {
                        CatFoodLog.i("Mihomo 本地端口 127.0.0.1:" + LOCAL_PORT + " 监听就绪！");
                        return;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignore) {}
                }
            });

            return true;

        } catch (Exception e) {
            CatFoodLog.e("启动 Mihomo 进程异常", e);
            return false;
        }
    }

    private static void applyLocalProxyToTelegram(String host, int port, String nodeName) {
        SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
        editor.putBoolean("proxy_enabled", true);
        editor.putString("proxy_ip", host);
        editor.putInt("proxy_port", port);
        editor.remove("proxy_secret");
        editor.remove("proxy_user");
        editor.remove("proxy_pass");
        editor.commit();

        SharedConfig.ProxyInfo localProxy = new SharedConfig.ProxyInfo(host, port, "⚡ 猫猫直连 (" + (TextUtils.isEmpty(nodeName) ? "默认节点" : nodeName) + ")", "", "");
        SharedConfig.currentProxy = SharedConfig.addProxy(localProxy);
        SharedConfig.saveProxyList();

        ConnectionsManager.setProxySettings(true, host, port, "", "", "");

        for (int a = 0; a < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; a++) {
            ConnectionsManager.native_resumeNetwork(a, false);
            try {
                ConnectionsManager.getInstance(a).updateDcSettings();
            } catch (Exception ignore) {}
        }

        CatFoodLog.i("Telegram 网络层已成功桥接至本地内核 127.0.0.1:" + port);
    }

    private static File getExecutableBinary(Context context) {
        try {
            // 1. 优先使用系统原生 lib 路径 (自带执行权限，SELinux 允许执行)
            String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
            CatFoodLog.i("检查原生库目录: " + nativeLibDir);
            if (!TextUtils.isEmpty(nativeLibDir)) {
                File nativeFile = new File(nativeLibDir, "libnekocore.so");
                CatFoodLog.i("原生内核路径: " + nativeFile.getAbsolutePath() + ", exists=" + nativeFile.exists() + ", canExec=" + nativeFile.canExecute());
                if (nativeFile.exists()) {
                    nativeFile.setExecutable(true, false);
                    return nativeFile;
                }
            }

            // 2. 备用路径：从 APK 解压到 files 目录
            File target = new File(context.getFilesDir(), "libnekocore.so");
            if (!target.exists() || target.length() < 1000) {
                String sourceDir = context.getApplicationInfo().sourceDir;
                CatFoodLog.i("从 APK 解压内核: " + sourceDir);
                try (ZipFile zip = new ZipFile(sourceDir)) {
                    ZipEntry entry = zip.getEntry("lib/arm64-v8a/libnekocore.so");
                    if (entry == null) {
                        entry = zip.getEntry("lib/armeabi-v7a/libnekocore.so");
                    }
                    if (entry != null) {
                        try (InputStream is = zip.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(target)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                }
            }
            if (target.exists()) {
                target.setExecutable(true, false);
                CatFoodLog.i("备用内核路径: " + target.getAbsolutePath() + ", exists=" + target.exists() + ", canExec=" + target.canExecute());
                return target;
            }
        } catch (Exception e) {
            CatFoodLog.e("提取内核二进制失败", e);
        }
        return null;
    }

    private static String generateClashConfig(String rawData, String selectedNode) {
        StringBuilder sb = new StringBuilder();
        sb.append("mixed-port: ").append(LOCAL_PORT).append("\n");
        sb.append("allow-lan: false\n");
        sb.append("mode: rule\n");
        sb.append("log-level: info\n");
        sb.append("external-controller: 127.0.0.1:9090\n\n");

        if (!TextUtils.isEmpty(rawData) && rawData.contains("proxies:")) {
            // 原生 Clash YAML 订阅
            int idx = rawData.indexOf("proxies:");
            String proxiesPart = rawData.substring(idx);
            // 截取只保留 proxies 块
            if (proxiesPart.contains("proxy-groups:")) {
                proxiesPart = proxiesPart.substring(0, proxiesPart.indexOf("proxy-groups:"));
            } else if (proxiesPart.contains("rules:")) {
                proxiesPart = proxiesPart.substring(0, proxiesPart.indexOf("rules:"));
            }
            sb.append(proxiesPart.trim()).append("\n\n");
        } else {
            // 尝试将 URI 或单行转换为 Clash 代理定义
            sb.append("proxies:\n");
            String proxyBlock = convertUriToClashProxy(rawData, selectedNode);
            sb.append(proxyBlock).append("\n\n");
        }

        sb.append("proxy-groups:\n");
        sb.append("  - name: PROXY\n");
        sb.append("    type: select\n");
        sb.append("    proxies:\n");
        if (!TextUtils.isEmpty(selectedNode)) {
            sb.append("      - \"").append(selectedNode.replace("\"", "\\\"")).append("\"\n");
        }
        sb.append("      - DIRECT\n\n");

        sb.append("rules:\n");
        sb.append("  - MATCH,PROXY\n");

        return sb.toString();
    }

    private static String convertUriToClashProxy(String uriStr, String fallbackName) {
        if (TextUtils.isEmpty(uriStr)) {
            return "  - name: \"DIRECT\"\n    type: direct";
        }
        try {
            String remark = fallbackName;
            if (uriStr.contains("#")) {
                String[] p = uriStr.split("#", 2);
                uriStr = p[0].trim();
                try {
                    remark = URLDecoder.decode(p[1].trim(), "UTF-8");
                } catch (Exception e) {
                    remark = p[1].trim();
                }
            }
            if (TextUtils.isEmpty(remark)) {
                remark = "Node-1";
            }

            if (uriStr.startsWith("hysteria2://") || uriStr.startsWith("hy2://")) {
                URI uri = new URI(uriStr);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 443;
                String auth = uri.getUserInfo() != null ? uri.getUserInfo() : "";
                Uri aUri = Uri.parse(uriStr);
                String sni = aUri.getQueryParameter("sni");
                boolean insecure = "1".equals(aUri.getQueryParameter("insecure"));

                StringBuilder sb = new StringBuilder();
                sb.append("  - name: \"").append(remark).append("\"\n");
                sb.append("    type: hysteria2\n");
                sb.append("    server: ").append(host).append("\n");
                sb.append("    port: ").append(port).append("\n");
                sb.append("    password: \"").append(auth).append("\"\n");
                if (!TextUtils.isEmpty(sni)) {
                    sb.append("    sni: ").append(sni).append("\n");
                }
                if (insecure) {
                    sb.append("    skip-cert-verify: true\n");
                }
                return sb.toString();
            } else if (uriStr.startsWith("vless://")) {
                URI uri = new URI(uriStr);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 443;
                String uuid = uri.getUserInfo() != null ? uri.getUserInfo() : "";
                Uri aUri = Uri.parse(uriStr);
                String sni = aUri.getQueryParameter("sni");
                String flow = aUri.getQueryParameter("flow");

                StringBuilder sb = new StringBuilder();
                sb.append("  - name: \"").append(remark).append("\"\n");
                sb.append("    type: vless\n");
                sb.append("    server: ").append(host).append("\n");
                sb.append("    port: ").append(port).append("\n");
                sb.append("    uuid: ").append(uuid).append("\n");
                sb.append("    tls: true\n");
                if (!TextUtils.isEmpty(sni)) {
                    sb.append("    servername: ").append(sni).append("\n");
                }
                if (!TextUtils.isEmpty(flow)) {
                    sb.append("    flow: ").append(flow).append("\n");
                }
                return sb.toString();
            } else if (uriStr.startsWith("trojan://")) {
                URI uri = new URI(uriStr);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 443;
                String password = uri.getUserInfo() != null ? uri.getUserInfo() : "";
                Uri aUri = Uri.parse(uriStr);
                String sni = aUri.getQueryParameter("sni");

                StringBuilder sb = new StringBuilder();
                sb.append("  - name: \"").append(remark).append("\"\n");
                sb.append("    type: trojan\n");
                sb.append("    server: ").append(host).append("\n");
                sb.append("    port: ").append(port).append("\n");
                sb.append("    password: \"").append(password).append("\n");
                if (!TextUtils.isEmpty(sni)) {
                    sb.append("    sni: ").append(sni).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception ignore) {}

        return "  - name: \"DIRECT\"\n    type: direct";
    }

    private static boolean isPortOpen(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
