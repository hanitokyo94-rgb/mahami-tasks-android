package pro.ktai.mahami;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/** منبه ملء الشاشة — يظهر فوق شاشة القفل مع رنين مستمر */
public class AlarmActivity extends Activity {
    private MediaPlayer player;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ringing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // تشغيل فوق شاشة القفل + إبقاء الشاشة مضاءة
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.alarm_activity);

        String title = getIntent().getStringExtra(ReminderReceiver.EXTRA_TITLE);
        String body = getIntent().getStringExtra(ReminderReceiver.EXTRA_BODY);
        TextView tvTitle = findViewById(R.id.alarmTitle);
        TextView tvBody = findViewById(R.id.alarmBody);
        tvTitle.setText(title == null ? "حان وقت المهمة" : title);
        tvBody.setText(body == null ? "" : body);

        startRinging();

        Button done = findViewById(R.id.alarmDone);
        Button snooze = findViewById(R.id.alarmSnooze);
        done.setOnClickListener(v -> {
            stopRinging();
            int id = getIntent().getIntExtra(ReminderReceiver.EXTRA_ID, 0);
            IntentHelper.openMainWithAction(this, id, ReminderReceiver.ACTION_DONE);
            finish();
        });
        snooze.setOnClickListener(v -> {
            stopRinging();
            int id = getIntent().getIntExtra(ReminderReceiver.EXTRA_ID, 0);
            IntentHelper.openMainWithAction(this, id, ReminderReceiver.ACTION_SNOOZE);
            finish();
        });
    }

    private void startRinging() {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player = new MediaPlayer();
            player.setDataSource(this, uri);
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            player = null;
        }
        vibrate();
    }

    private void vibrate() {
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator == null) return;
            long[] pattern = {0, 500, 250, 500, 250, 500};
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception ignored) {}
    }

    private void stopRinging() {
        ringing = false;
        try { if (player != null) { player.stop(); player.release(); } } catch (Exception ignored) {}
        player = null;
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) {}
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        if (ringing) stopRinging();
        super.onDestroy();
    }
}
