package pro.ktai.mahami;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** مخزن المنبهات + الجدولة عبر AlarmManager */
public class ReminderStore {
    private static final String PREFS = "mahami_reminders";
    private static final String KEY = "pending";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** استبدال كل المنبهات المجدولة بقائمة جديدة قادمة من الويب */
    public static void replaceAll(Context context, String json) {
        SharedPreferences p = prefs(context);
        SharedPreferences.Editor e = p.edit();
        // ألغِ القديمة
        try {
            JSONArray old = new JSONArray(p.getString(KEY, "[]"));
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            for (int i = 0; i < old.length(); i++) {
                JSONObject o = old.getJSONObject(i);
                cancel(context, am, o.getInt("id"));
            }
        } catch (Exception ignored) {}
        e.putString(KEY, json);
        e.apply();
        // جدّل الجديدة
        try {
            JSONArray arr = new JSONArray(json);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            int scheduled = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long ts = o.getLong("ts");
                if (ts <= System.currentTimeMillis()) continue;
                schedule(context, am, o.getInt("id"), o.optString("title", "حان وقت المهمة"),
                        o.optString("body", ""), ts);
                scheduled++;
            }
            android.util.Log.i("Mahami", "scheduled " + scheduled + " reminders");
        } catch (Exception ex) {
            android.util.Log.e("Mahami", "replaceAll error", ex);
        }
    }

    public static void cancelOne(Context context, int id) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            cancel(context, am, id);
            SharedPreferences p = prefs(context);
            JSONArray arr = new JSONArray(p.getString(KEY, "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getInt("id") != id) out.put(arr.getJSONObject(i));
            }
            p.edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static void cancel(Context context, AlarmManager am, int id) {
        Intent i = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) { am.cancel(pi); pi.cancel(); }
    }

    private static void schedule(Context context, AlarmManager am, int id, String title, String body, long ts) {
        Intent i = new Intent(context, ReminderReceiver.class);
        i.putExtra(ReminderReceiver.EXTRA_ID, id);
        i.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        i.putExtra(ReminderReceiver.EXTRA_BODY, body);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        boolean exactAllowed = true;
        if (Build.VERSION.SDK_INT >= 31) {
            exactAllowed = am.canScheduleExactAlarms();
        }
        try {
            if (exactAllowed) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
            }
        } catch (SecurityException se) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
        }
    }

    /** إعادة جدولة كل المنبهات المحفوظة (بعد إعادة تشغيل الجهاز) */
    public static void rescheduleAllFromStore(Context context) {
        try {
            String json = prefs(context).getString(KEY, "[]");
            JSONArray arr = new JSONArray(json);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                long ts = o.getLong("ts");
                if (ts <= System.currentTimeMillis()) continue;
                schedule(context, am, o.getInt("id"), o.optString("title", "حان وقت المهمة"),
                        o.optString("body", ""), ts);
            }
        } catch (Exception ex) {
            android.util.Log.e("Mahami", "rescheduleAllFromStore error", ex);
        }
    }

    /** نص حالة للعرض في الإعدادات */
    public static String statusText(Context context) {
        try {
            int n = prefs(context).getString(KEY, "[]").length();            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            boolean exact = true;
            if (Build.VERSION.SDK_INT >= 31) exact = am.canScheduleExactAlarms();
            SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "منبهات مجدولة: " + n + " · دقة المنبه: " + (exact ? "دقيقة" : "تقريبية") + " · " + f.format(new Date());
        } catch (Exception e) {
            return "";
        }
    }
}
