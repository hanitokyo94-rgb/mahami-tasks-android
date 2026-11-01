package pro.ktai.mahami;

import android.app.Activity;
import android.content.Intent;

/** أدوات Intent مشتركة */
public class IntentHelper {
    public static void openMainWithAction(Activity activity, int id, String action) {
        Intent i = new Intent(activity, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra(ReminderReceiver.EXTRA_ID, id);
        i.putExtra(ReminderReceiver.EXTRA_ACTION, action);
        activity.startActivity(i);
    }
}
