package pro.ktai.mahami;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/** شاشة قفل التطبيق برمز PIN */
public class LockActivity extends Activity {
    private StringBuilder entered = new StringBuilder();
    private String pin;
    private boolean unlockMode;
    private TextView dots;
    private EditText[] newPin = new EditText[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        SharedPreferences p = getSharedPreferences("mahami_prefs", MODE_PRIVATE);
        pin = p.getString("lock_pin", "");
        unlockMode = "unlock".equals(getIntent().getStringExtra("mode"));

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setBackgroundColor(0xFF1A0505);
        root.setPadding(dp(32), dp(32), dp(32), dp(32));

        TextView title = new TextView(this);
        title.setText(unlockMode ? "🔒 أدخل الرمز لفتح التطبيق" : "🔒 اختر رمزاً جديداً (4 أرقام)");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);

        dots = new TextView(this);
        dots.setText("— — — —");
        dots.setTextColor(0xFFE05555);
        dots.setTextSize(30);
        dots.setGravity(android.view.Gravity.CENTER);
        dots.setPadding(0, dp(24), 0, dp(24));
        root.addView(dots);

        if (unlockMode) {
            // لوحة أرقام
            android.widget.GridLayout grid = new android.widget.GridLayout(this);
            grid.setColumnCount(3);
            for (int i = 1; i <= 9; i++) {
                Button b = numBtn(String.valueOf(i));
                android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
                lp.width = dp(88); lp.height = dp(64);
                lp.columnSpec = android.widget.GridLayout.spec((i - 1) % 3);
                lp.rowSpec = android.widget.GridLayout.spec((i - 1) / 3);
                b.setLayoutParams(lp);
                grid.addView(b);
            }
            Button zero = numBtn("0");
            android.widget.GridLayout.LayoutParams zp = new android.widget.GridLayout.LayoutParams();
            zp.width = dp(88); zp.height = dp(64);
            zp.columnSpec = android.widget.GridLayout.spec(1);
            zp.rowSpec = android.widget.GridLayout.spec(3);
            zero.setLayoutParams(zp);
            grid.addView(zero);
            root.addView(grid);
        } else {
            // وضع التعيين: حقلان
            for (int i = 0; i < 2; i++) {
                newPin[i] = new EditText(this);
                newPin[i].setHint(i == 0 ? "الرمز" : "تأكيد الرمز");
                newPin[i].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                newPin[i].setMaxLines(1);
                newPin[i].setTextColor(0xFFFFFFFF);
                newPin[i].setHintTextColor(0xFF888888);
                root.addView(newPin[i]);
            }
            Button save = new Button(this);
            save.setText("حفظ الرمز");
            save.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String a = newPin[0].getText().toString().trim();
                    String b = newPin[1].getText().toString().trim();
                    if (a.length() < 4) { Toast.makeText(LockActivity.this, "الرمز 4 أرقام على الأقل", Toast.LENGTH_SHORT).show(); return; }
                    if (!a.equals(b)) { Toast.makeText(LockActivity.this, "الرمزان غير متطابقين", Toast.LENGTH_SHORT).show(); return; }
                    getSharedPreferences("mahami_prefs", MODE_PRIVATE).edit().putString("lock_pin", a).putBoolean("lock_enabled", true).apply();
                    Toast.makeText(LockActivity.this, "تم تفعيل القفل ✅", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
            root.addView(save);
        }
        setContentView(root);
    }

    private Button numBtn(final String d) {
        Button b = new Button(this);
        b.setText(d);
        b.setTextSize(20);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                entered.append(d);
                updateDots();
                if (entered.length() >= pin.length() && pin.length() > 0) {
                    if (entered.toString().equals(pin)) {
                        finish();
                    } else {
                        Toast.makeText(LockActivity.this, "رمز خاطئ", Toast.LENGTH_SHORT).show();
                        entered.setLength(0);
                        updateDots();
                    }
                }
            }
        });
        return b;
    }

    private void updateDots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(i < entered.length() ? "● " : "— ");
        dots.setText(sb.toString().trim());
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
