package pro.ktai.mahami;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** ودجت الشاشة الرئيسية — يعرض مهام اليوم وعداد المنبه القادم */
public class TasksWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "mahami_widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager awm, int[] appWidgetIds) {
        pushUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        pushUpdate(context);
    }

    /** يقرأ ملخص اليوم (يكتبه JS عبر الجسر) ويحدّث الودجت */
    public static void pushUpdate(Context context) {
        android.content.SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String summary = p.getString("summary", "");
        String next = p.getString("next", "");
        int done = p.getInt("done", 0);
        int total = p.getInt("total", 0);

        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.tasks_widget);
        v.setTextViewText(R.id.wDate, widgetDate());
        v.setTextViewText(R.id.wSummary, summary.isEmpty() ? "لا مهام لليوم" : summary);
        v.setTextViewText(R.id.wNext, next.isEmpty() ? "لا منبهات قادمة" : next);
        v.setTextViewText(R.id.wCount, done + "/" + total);

        Intent open = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (open == null) open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.wRoot, pi);

        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        awm.updateAppWidget(awm.getAppWidgetIds(
                new android.content.ComponentName(context, TasksWidgetProvider.class)), v);
    }

    /** JS يكتب الملخص عبر الجسر */
    public static void saveSummary(Context context, String summary, String next, int done, int total) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("summary", summary).putString("next", next)
                .putInt("done", done).putInt("total", total).apply();
        pushUpdate(context);
    }

    private static String widgetDate() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        String[] days = {"الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"};
        String[] months = {"يناير","فبراير","مارس","أبريل","مايو","يونيو","يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"};
        return days[c.get(java.util.Calendar.DAY_OF_WEEK) - 1] + " "
                + c.get(java.util.Calendar.DAY_OF_MONTH) + " "
                + months[c.get(java.util.Calendar.MONTH)];
    }
}
