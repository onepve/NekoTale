package tw.nekomimi.nekogram;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CrashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String report = getIntent() != null ? getIntent().getStringExtra("crash_report") : "无崩溃详情";

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1E293B);
        root.setPadding(48, 80, 48, 48);

        TextView title = new TextView(this);
        title.setText("🐱 猫猫物语 启动异常捕获");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("应用在启动或运行时捕获到未处理异常，请点击下方「复制日志」反馈：");
        subtitle.setTextColor(0xFF94A3B8);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, 0, 0, 24);
        root.addView(subtitle);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        scrollView.setLayoutParams(scrollParams);
        scrollView.setBackgroundColor(0xFF0F172A);
        scrollView.setPadding(24, 24, 24, 24);

        TextView logView = new TextView(this);
        logView.setText(report);
        logView.setTextColor(0xFFE2E8F0);
        logView.setTextSize(12);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setTextIsSelectable(true);
        scrollView.addView(logView);
        root.addView(scrollView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, 32, 0, 0);

        Button copyBtn = new Button(this);
        copyBtn.setText("复制崩溃日志");
        copyBtn.setBackgroundColor(0xFF3B82F6);
        copyBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        btnParams.setMargins(0, 0, 16, 0);
        copyBtn.setLayoutParams(btnParams);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("NekoTale Crash", report));
                Toast.makeText(this, "崩溃日志已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
        btnRow.addView(copyBtn);

        Button closeBtn = new Button(this);
        closeBtn.setText("退出应用");
        closeBtn.setBackgroundColor(0xFF64748B);
        closeBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        closeParams.setMargins(16, 0, 0, 0);
        closeBtn.setLayoutParams(closeParams);
        closeBtn.setOnClickListener(v -> {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
        btnRow.addView(closeBtn);

        root.addView(btnRow);
        setContentView(root);
    }
}
