package pro.ktai.mahami;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        web.setBackgroundColor(Color.parseColor("#F4F6F5"));
        web.addJavascriptInterface(new Bridge(), "AndroidNotifications");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override public void run() { request.grant(request.getResources()); }
                });
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/www/index.html");

        // طلب إذن الإشعارات (أندرويد 13+)
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100 && web != null) {
            web.evaluateJavascript("document.dispatchEvent(new Event('android-perms'))", null);
        }
    }

    private class Bridge {
        @JavascriptInterface
        public String hasPermission() {
            if (Build.VERSION.SDK_INT < 33) return "true";
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED ? "true" : "false";
        }

        @JavascriptInterface
        public void requestPermission() {
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
                    }
                });
            }
        }

        @JavascriptInterface
        public String canScheduleExact() {
            if (Build.VERSION.SDK_INT < 31) return "true";
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            return am != null && am.canScheduleExactAlarms() ? "true" : "false";
        }

        @JavascriptInterface
        public void requestExactAlarm() {
            if (Build.VERSION.SDK_INT >= 31) {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (am != null && !am.canScheduleExactAlarms()) {
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Exception ignored) {}
                }
            }
        }

        /** استبدال كل المنبهات: [{"id":123,"title":"...","body":"...","ts":1690000000000}] */
        @JavascriptInterface
        public void rescheduleAll(String json) {
            ReminderStore.replaceAll(MainActivity.this, json);
        }

        @JavascriptInterface
        public void cancel(final String id) {
            try { ReminderStore.cancelOne(MainActivity.this, Integer.parseInt(id)); } catch (Exception ignored) {}
        }

        /** إشعار تجريبي فوري */
        @JavascriptInterface
        public void test() {
            ReminderReceiver.showNotification(MainActivity.this, 999999,
                    "تنبيه تجريبي ✓", "الإشعارات تعمل — هكذا سيصلك تنبيه المهام");
        }

        @JavascriptInterface
        public String status() {
            return ReminderStore.statusText(MainActivity.this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
