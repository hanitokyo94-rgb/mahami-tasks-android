package pro.ktai.mahami;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "mahami_reminders";
    public static final String ALARM_CHANNEL_ID = "mahami_alarm";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_ALARM = "alarm";
    public static final String ACTION_DONE = "done";
    public static final String ACTION_SNOOZE = "snooze";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ReminderStore.rescheduleAllFromStore(context);
            return;
        }
        int id = intent.getIntExtra(EXTRA_ID, 0);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        boolean alarm = intent.getBooleanExtra(EXTRA_ALARM, false);
        String btn = intent.getStringExtra(EXTRA_ACTION);
        if (ACTION_DONE.equals(btn)) {
            ReminderStore.updateStatus(context, id, "done");
            TasksWidgetProvider.pushUpdate(context);
            return;
        }
        if (ACTION_SNOOZE.equals(btn)) {
            long at = System.currentTimeMillis() + 10 * 60 * 1000L;
            ReminderStore.rescheduleOne(context, id, title, body, at, alarm);
            return;
        }
        showNotification(context, id, title, body, alarm);
    }

    static void showNotification(Context context, int id, String title, String body) {
        showNotification(context, id, title, body, false);
    }

    static void showNotification(Context context, int id, String title, String body, boolean taskAlarm) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        SharedPreferences p = context.getSharedPreferences("mahami_prefs", Context.MODE_PRIVATE);
        boolean alarmMode = taskAlarm || p.getBoolean("notif_alarm_mode", false);
        boolean sound = p.getBoolean("notif_sound", true);
        boolean vibrate = p.getBoolean("notif_vibrate", true);
        String fTitle = (title == null || title.isEmpty()) ? "حان وقت المهمة" : title;
        String fBody = body == null ? "" : body;

        ensureChannels(nm, alarmMode, sound, vibrate);
        String channelId = alarmMode ? ALARM_CHANNEL_ID : CHANNEL_ID;

        Intent open = new Intent(context, AlarmActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        open.putExtra(EXTRA_ID, id);
        open.putExtra(EXTRA_TITLE, fTitle);
        open.putExtra(EXTRA_BODY, fBody);
        PendingIntent fullScreenPi = PendingIntent.getActivity(context, id + 100000, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent tap = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (tap == null) tap = new Intent(context, MainActivity.class);
        tap.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tapPi = PendingIntent.getActivity(context, id, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // زر: تم
        Intent doneI = new Intent(context, ReminderReceiver.class);
        doneI.setAction("pro.ktai.mahami.NOTIF_ACTION");
        doneI.putExtra(EXTRA_ID, id);
        doneI.putExtra(EXTRA_TITLE, fTitle);
        doneI.putExtra(EXTRA_BODY, fBody);
        doneI.putExtra(EXTRA_ACTION, ACTION_DONE);
        PendingIntent donePi = PendingIntent.getBroadcast(context, id + 200000, doneI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // زر: تأجيل 10 دقائق
        Intent snoozeI = new Intent(context, ReminderReceiver.class);
        snoozeI.setAction("pro.ktai.mahami.NOTIF_ACTION");
        snoozeI.putExtra(EXTRA_ID, id);
        snoozeI.putExtra(EXTRA_TITLE, fTitle);
        snoozeI.putExtra(EXTRA_BODY, fBody);
        snoozeI.putExtra(EXTRA_ACTION, ACTION_SNOOZE);
        PendingIntent snoozePi = PendingIntent.getBroadcast(context, id + 300000, snoozeI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b = new Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(fTitle)
                .setContentText(fBody)
                .setStyle(new Notification.BigTextStyle().bigText(fBody))
                .setContentIntent(tapPi)
                .addAction(new Notification.Action.Builder(null, "تم", donePi).build())
                .addAction(new Notification.Action.Builder(null, "تأجيل 10د", snoozePi).build())
                .setAutoCancel(true)
                .setCategory(alarmMode ? Notification.CATEGORY_ALARM : Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_MAX);
        if (alarmMode) b.setFullScreenIntent(fullScreenPi, true);
        Notification n = b.build();
        try { nm.notify(id, n); } catch (Exception ignored) {}
    }

    static void ensureChannels(NotificationManager nm, boolean alarmMode, boolean sound, boolean vibrate) {
        // قناة التنبيهات العادية
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "تنبيهات المهام",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("تنبيهات مواعيد المهام");
        ch.enableVibration(vibrate);
        ch.enableLights(true);
        if (sound) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            ch.setSound(soundUri, attrs);
        } else {
            ch.setSound(null, null);
        }
        nm.createNotificationChannel(ch);

        // قناة المنبه (شاشة كاملة + صوت منبه)
        NotificationChannel al = new NotificationChannel(ALARM_CHANNEL_ID, "منبه المهمة (شاشة كاملة)",
                NotificationManager.IMPORTANCE_HIGH);
        al.setDescription("منبه يظهر في منتصف الشاشة مع رنين");
        al.setBypassDnd(true);
        al.enableVibration(vibrate);
        al.enableLights(true);
        al.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        if (sound) {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            al.setSound(alarmUri, attrs);
        } else {
            al.setSound(null, null);
        }
        nm.createNotificationChannel(al);
    }
}
