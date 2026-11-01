package pro.ktai.mahami;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("mahami_prefs", MODE_PRIVATE);

        // قفل التطبيق: شاشة القفل قبل الواجهة
        if (prefs.getBoolean("lock_enabled", false)) {
            String pin = prefs.getString("lock_pin", "");
            String action = getIntent() != null ? getIntent().getStringExtra(ReminderReceiver.EXTRA_ACTION) : null;
            if (!ReminderReceiver.ACTION_DONE.equals(action)) {
                Intent li = new Intent(this, LockActivity.class);
                li.putExtra("mode", "unlock");
                startActivity(li);
            }
        }

        web = new WebView(this);
        setContentView(web);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, 0); // لا شيء مميز

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setDatabaseEnabled(true);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "AndroidNotifications");
        web.loadUrl("file:///android_asset/www/index.html");

        // تمرير إجراء الإشعار (تم) إلى الواجهة
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        final String action = intent.getStringExtra(ReminderReceiver.EXTRA_ACTION);
        if (action != null && web != null) {
            final int tid = intent.getIntExtra(ReminderReceiver.EXTRA_ID, 0);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    web.evaluateJavascript("window.__notifAction && window.__notifAction('" + action + "','" + tid + "')", null);
                }
            }, 800);
        }
        TasksWidgetProvider.pushUpdate(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.__appResume && window.__appResume()", null);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    class Bridge {
        @JavascriptInterface
        public String getTasks() {
            return getSharedPreferences("mahami_tasks", MODE_PRIVATE).getString("tasks", "[]");
        }

        @JavascriptInterface
        public void saveTasks(final String json) {
            getSharedPreferences("mahami_tasks", MODE_PRIVATE).edit().putString("tasks", json).apply();
            ReminderStore.replaceAll(MainActivity.this, extractReminders(json));
            TasksWidgetProvider.pushUpdate(MainActivity.this);
        }

        @JavascriptInterface
        public void rescheduleAll(final String json) {
            ReminderStore.replaceAll(MainActivity.this, extractReminders(json));
            TasksWidgetProvider.pushUpdate(MainActivity.this);
        }

        @JavascriptInterface
        public void cancel(final int id) {
            ReminderStore.cancelOne(MainActivity.this, id);
        }

        @JavascriptInterface
        public void setTheme(final String theme, final String accent) {
            prefs.edit().putString("theme", theme).putString("accent", accent).apply();
        }

        @JavascriptInterface
        public boolean hasPermission() {
            if (Build.VERSION.SDK_INT >= 33) {
                return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ? true : false;
            }
            return true;
        }

        @JavascriptInterface
        public void requestPermission() {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        @JavascriptInterface
        public boolean canScheduleExact() {
            if (Build.VERSION.SDK_INT >= 31) {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                return am.canScheduleExactAlarms();
            }
            return true;
        }

        @JavascriptInterface
        public void requestExactAlarm() {
            if (Build.VERSION.SDK_INT >= 31) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "افتح الإعدادات ← التطبيقات ← مهامي ← منبهات ومواعيد", Toast.LENGTH_LONG).show();
                }
            }
        }

        @JavascriptInterface
        public void test() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    ReminderReceiver.showNotification(MainActivity.this, 999001, "تنبيه تجريبي 🔔", "هكذا سيظهر تنبيه مهامك");
                }
            });
        }

        @JavascriptInterface
        public void testAlarm() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    ReminderReceiver.showNotification(MainActivity.this, 999002, "⏰ منبه تجريبي", "هكذا يظهر المنبه في منتصف الشاشة");
                }
            });
        }

        @JavascriptInterface
        public String status() {
            return ReminderStore.statusText(MainActivity.this);
        }

        @JavascriptInterface
        public void setNotifPrefs(final boolean alarmMode, final boolean sound, final boolean vibrate) {
            prefs.edit().putBoolean("notif_alarm_mode", alarmMode)
                    .putBoolean("notif_sound", sound)
                    .putBoolean("notif_vibrate", vibrate).apply();
        }

        @JavascriptInterface
        public void setLock(final boolean enabled, final String pin) {
            prefs.edit().putBoolean("lock_enabled", enabled).putString("lock_pin", pin).apply();
        }

        @JavascriptInterface
        public String getLock() {
            return (prefs.getBoolean("lock_enabled", false) ? "1" : "0") + "|" + prefs.getString("lock_pin", "");
        }

        @JavascriptInterface
        public void widgetSync(String json) {
            try {
                org.json.JSONObject o = new org.json.JSONObject(json == null ? "{}" : json);
                TasksWidgetProvider.saveSummary(MainActivity.this,
                        o.optString("summary", ""), o.optString("next", ""),
                        o.optInt("done", 0), o.optInt("total", 0));
            } catch (Exception e) {
                TasksWidgetProvider.pushUpdate(MainActivity.this);
            }
        }

        @JavascriptInterface
        public void openWidgetHelp() {
            Toast.makeText(MainActivity.this, "اضغط مطولاً على الشاشة الرئيسية ← Widgets ← مهامي", Toast.LENGTH_LONG).show();
        }
    }

    /** تحويل معرف المهمة (نصي أو رقمي) إلى int */
    private static int parseId(Object o) {
        if (o == null) return 0;
        try {
            return (int) Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return Math.abs(String.valueOf(o).hashCode());
        }
    }

    /** استخراج المنبهات القادمة من JSON المهام (الصيغة: date/time/offset) */
    private static String extractReminders(String tasksJson) {
        try {
            org.json.JSONArray tasks = new org.json.JSONArray(tasksJson);
            org.json.JSONArray out = new org.json.JSONArray();
            java.util.TimeZone tz = java.util.TimeZone.getDefault();
            java.util.Calendar cal = java.util.Calendar.getInstance(tz);
            long now = System.currentTimeMillis();
            for (int i = 0; i < tasks.length(); i++) {
                org.json.JSONObject t = tasks.getJSONObject(i);
                boolean done = t.optBoolean("done", false) || "done".equals(t.optString("status"));
                String date = t.optString("date", "");
                String time = t.optString("time", "");
                boolean remind = t.optBoolean("remind", false);
                if (done || !remind || date.isEmpty() || time.isEmpty()) continue;
                String[] d = date.split("-");
                String[] tm = time.split(":");
                if (d.length < 3 || tm.length < 2) continue;
                cal.clear();
                cal.set(Integer.parseInt(d[0]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2]),
                        Integer.parseInt(tm[0]), Integer.parseInt(tm[1]), 0);
                long ts = cal.getTimeInMillis() - t.optLong("offset", 0) * 60000L;
                if (ts <= now) continue;
                String title = t.optString("title", "حان وقت المهمة");
                String body = t.optString("notes", "");
                boolean alarm = t.optBoolean("alarm", false);
                org.json.JSONObject r = new org.json.JSONObject();
                r.put("id", parseId(t.opt("id")));
                r.put("title", (alarm ? "⏰ " : "🔔 ") + title);
                r.put("body", body);
                r.put("ts", ts);
                r.put("alarm", alarm);
                out.put(r);
            }
            return out.toString();
        } catch (Exception e) {
            return "[]";
        }
    }
}
