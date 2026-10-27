package com.eujor.seasonalarm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class RingActivity extends Activity {
    private long id;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.configure(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        id = getIntent().getLongExtra(AlarmScheduler.EXTRA_ID, -1);
        Alarm a = new AlarmRepository(this).find(id);
        if (a == null) { finish(); return; }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 24), Ui.dp(this, 52), Ui.dp(this, 24), Ui.dp(this, 34));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Ui.BRAND_DARK);

        TextView state = Ui.badge(this, "ALARM RINGING", Color.WHITE, Ui.BRAND);
        root.addView(state);

        TextView label = Ui.text(this, a.label, 27, Color.WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, Ui.dp(this, 24), 0, Ui.dp(this, 4));
        root.addView(label);

        TextView time = Ui.text(this, String.format("%02d:%02d", a.hour, a.minute), 58, Color.WHITE, true);
        time.setGravity(Gravity.CENTER);
        root.addView(time);

        TextView city = Ui.text(this, a.cityName, 16, 0xffb9d5d1, true);
        city.setGravity(Gravity.CENTER);
        root.addView(city);

        ZonedDateTime homeNow = ZonedDateTime.now(TimeZones.resolveZone(TimeZones.HOME_ZONE));
        TextView home = Ui.text(this, "Device time  •  " + homeNow.format(DateTimeFormatter.ofPattern("HH:mm:ss z")), 13, 0xff8ebdb6, false);
        home.setGravity(Gravity.CENTER);
        home.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 32));
        root.addView(home);

        Button snooze = Ui.primaryButton(this, "Snooze " + a.snoozeMinutes + " min");
        snooze.setOnClickListener(v -> {
            action(AlarmRingingService.ACTION_SNOOZE);
            finish();
        });
        root.addView(snooze);

        Button dismiss = Ui.secondaryButton(this, "Dismiss alarm");
        dismiss.setTextColor(Color.WHITE);
        dismiss.setBackground(Ui.rounded(0xff173740, 16, 0xff31535b, 1, this));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 52));
        dp.setMargins(0, Ui.dp(this, 6), 0, 0);
        dismiss.setLayoutParams(dp);
        dismiss.setOnClickListener(v -> {
            action(AlarmRingingService.ACTION_STOP);
            finish();
        });
        root.addView(dismiss);

        String soundState = AlarmRingingService.SILENT_TONE.equals(a.toneUri) ? "Silent tone" : "Tone active";
        TextView hint = Ui.text(this, soundState + (a.vibrate ? " • vibration active" : " • vibration off"), 11, 0xff7fa9a3, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(this, 18), 0, 0);
        root.addView(hint);

        setContentView(root);
    }

    private void action(String action) {
        startForegroundService(new Intent(this, AlarmRingingService.class)
                .setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ID, id));
    }
}
