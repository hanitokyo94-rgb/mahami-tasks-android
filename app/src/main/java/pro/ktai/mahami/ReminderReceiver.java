package pro.ktai.mahami;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "mahami_reminders";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // بعد إعادة تشغيل الجهاز: أعد جدولة كل المنبهات من المخزن
            ReminderStore.rescheduleAllFromStore(context);
            return;
        }
        int id = intent.getIntExtra(EXTRA_ID, 0);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String body = intent.getStringExtra(EXTRA_BODY);
        showNotification(context, id, title, body);
    }

    static void showNotification(Context context, int id, String title, String body) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        ensureChannel(nm);
        Intent open = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (open == null) open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title == null ? "حان وقت المهمة" : title)
                .setContentText(body == null ? "" : body)
                .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();
        try { nm.notify(id, n); } catch (Exception ignored) {}
    }

    static void ensureChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "تنبيهات المهام",
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("تنبيهات مواعيد المهام — تظهر حتى والتطبيق مغلق");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 300, 150, 300});
        ch.enableLights(true);
        nm.createNotificationChannel(ch);
    }
}
